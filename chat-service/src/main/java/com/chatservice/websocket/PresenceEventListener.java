package com.chatservice.websocket;

import com.chatservice.service.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresenceEventListener {

    private static final Logger log = LoggerFactory.getLogger(PresenceEventListener.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final RedisCacheService     cacheService;

    // username -> sessionId (supports multiple tabs: last one wins)
    private final Map<String, String> userSessionMap = new ConcurrentHashMap<>();

    // sessionId -> username (for quick disconnect lookup)
    private final Map<String, String> sessionUserMap = new ConcurrentHashMap<>();

    public PresenceEventListener(SimpMessagingTemplate messagingTemplate,
                                 RedisCacheService cacheService) {
        this.messagingTemplate = messagingTemplate;
        this.cacheService      = cacheService;
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() == null) return;

        String username  = accessor.getUser().getName(); // JWT principal = username
        String sessionId = accessor.getSessionId();

        userSessionMap.put(username, sessionId);
        sessionUserMap.put(sessionId, username);

        // Mark online in Redis (TTL = 60 s, refreshed by heartbeat)
        cacheService.setUserOnline(username);

        log.info("PRESENCE CONNECT  username={} session={}", username, sessionId);

        // Broadcast to ALL clients: this user came online
        broadcastPresenceEvent(username, true, null);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        String username = sessionUserMap.remove(sessionId);
        if (username == null) return;

        // Only mark offline if this was the user's last session
        String currentSession = userSessionMap.get(username);
        if (sessionId.equals(currentSession)) {
            userSessionMap.remove(username);

            String lastSeen = Instant.now().toString();
            cacheService.setUserOffline(username, lastSeen);

            log.info("PRESENCE DISCONNECT username={} lastSeen={}", username, lastSeen);

            // Broadcast to ALL clients: this user went offline with lastSeen timestamp
            broadcastPresenceEvent(username, false, lastSeen);
        }
    }

    /**
     * Broadcasts a single user's presence change to /topic/presence.
     * Payload: { username, online, lastSeen }
     */
    private void broadcastPresenceEvent(String username, boolean online, String lastSeen) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("username", username);
            payload.put("online",   online);
            payload.put("lastSeen", lastSeen); // null when online, ISO string when offline

            messagingTemplate.convertAndSend("/topic/presence", payload);
        } catch (Exception e) {
            log.warn("Failed to broadcast presence for {}: {}", username, e.getMessage());
        }
    }

    /** Returns the set of currently online usernames (for REST endpoint if needed) */
    public Set<String> getOnlineUsernames() {
        return userSessionMap.keySet();
    }
}