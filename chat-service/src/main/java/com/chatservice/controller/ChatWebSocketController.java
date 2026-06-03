package com.chatservice.controller;

import com.chatservice.dto.ChatDtos.WsMessage;
import com.chatservice.websocket.PresenceEventListener;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;
    private final PresenceEventListener presenceEventListener;

    public ChatWebSocketController(
            SimpMessagingTemplate messagingTemplate,
            PresenceEventListener presenceEventListener
    ) {
        this.messagingTemplate    = messagingTemplate;
        this.presenceEventListener = presenceEventListener;
    }

    // ─────────────────────────────────────────────
    // SEND MESSAGE
    // Client sends to: /app/chat.send
    // ─────────────────────────────────────────────
    @MessageMapping("/chat.send")
    public void handleMessage(@Payload WsMessage message, Principal principal) {
        messagingTemplate.convertAndSend(
                "/topic/chat/" + message.getChatId(), message);
    }

    // ─────────────────────────────────────────────
    // TYPING START
    // Client sends to: /app/chat.typing
    // ─────────────────────────────────────────────
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload Map<String, String> payload) {
        String chatId   = payload.get("chatId");
        String username = payload.get("username");
        Map<String, Object> response = new HashMap<>();
        response.put("type",     "TYPING");
        response.put("chatId",   chatId);
        response.put("username", username);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);
    }

    // ─────────────────────────────────────────────
    // TYPING STOP
    // Client sends to: /app/chat.stopTyping
    // ─────────────────────────────────────────────
    @MessageMapping("/chat.stopTyping")
    public void handleStopTyping(@Payload Map<String, String> payload) {
        String chatId   = payload.get("chatId");
        String username = payload.get("username");
        Map<String, Object> response = new HashMap<>();
        response.put("type",     "STOP_TYPING");
        response.put("chatId",   chatId);
        response.put("username", username);
        messagingTemplate.convertAndSend("/topic/chat/" + chatId, response);
    }

    // ─────────────────────────────────────────────
    // PRESENCE SYNC
    // Client sends to: /app/presence.sync after connecting.
    // Server replies ONLY to that user with all currently online users.
    // ─────────────────────────────────────────────
    @MessageMapping("/presence.sync")
    public void handlePresenceSync(Principal principal) {
        if (principal == null) return;

        Set<String> onlineUsernames = presenceEventListener.getOnlineUsernames();

        List<Map<String, Object>> snapshot = onlineUsernames.stream()
            .map(username -> {
                Map<String, Object> entry = new HashMap<>();
                entry.put("username", username);
                entry.put("online",   true);
                entry.put("lastSeen", null);
                return entry;
            })
            .collect(Collectors.toList());

        Map<String, Object> responsePayload = new HashMap<>();
        responsePayload.put("type",     "PRESENCE_SNAPSHOT");
        responsePayload.put("snapshot", snapshot);

        messagingTemplate.convertAndSendToUser(
            principal.getName(),
            "/queue/presence.snapshot",
            responsePayload
        );
    }
}