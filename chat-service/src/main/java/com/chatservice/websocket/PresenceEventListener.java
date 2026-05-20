package com.chatservice.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PresenceEventListener {

    private static final Logger log = LoggerFactory.getLogger(PresenceEventListener.class);
    private final SimpMessagingTemplate messagingTemplate;

    // Track online usernames
    private final Set<String> onlineUsers = ConcurrentHashMap.newKeySet();

    public PresenceEventListener(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() != null) {
            String userId = accessor.getUser().getName();
            onlineUsers.add(userId);
            log.info("User connected: {}", userId);
            broadcastOnlineUsers();
        }
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() != null) {
            String userId = accessor.getUser().getName();
            onlineUsers.remove(userId);
            log.info("User disconnected: {}", userId);
            broadcastOnlineUsers();
        }
    }

    private void broadcastOnlineUsers() {
        try {
            messagingTemplate.convertAndSend("/topic/presence", onlineUsers);
        } catch (Exception e) {
            log.warn("Failed to broadcast presence: {}", e.getMessage());
        }
    }
}