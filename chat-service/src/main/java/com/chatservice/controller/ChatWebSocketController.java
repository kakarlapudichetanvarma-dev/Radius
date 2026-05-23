package com.chatservice.controller;

import com.chatservice.dto.ChatDtos.WsMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(
            SimpMessagingTemplate messagingTemplate
    ) {
        this.messagingTemplate = messagingTemplate;
    }

    // ─────────────────────────────────────────────
    // SEND MESSAGE
    // Client sends to: /app/chat.send
    // ─────────────────────────────────────────────
    @MessageMapping("/chat.send")
    public void handleMessage(
            @Payload WsMessage message,
            Principal principal
    ) {

        messagingTemplate.convertAndSend(
                "/topic/chat/" + message.getChatId(),
                message
        );
    }

    // ─────────────────────────────────────────────
    // TYPING START
    // Client sends to: /app/chat.typing
    // ─────────────────────────────────────────────
   @MessageMapping("/chat.typing")
public void handleTyping(
        @Payload Map<String, String> payload
) {

    String chatId = payload.get("chatId");
    String username = payload.get("username");

    Map<String, Object> response = new HashMap<>();

    response.put("type", "TYPING");
    response.put("chatId", chatId);
    response.put("username", username);

    messagingTemplate.convertAndSend(
            "/topic/chat/" + chatId,
            response
    );
}

@MessageMapping("/chat.stopTyping")
public void handleStopTyping(
        @Payload Map<String, String> payload
) {

    String chatId = payload.get("chatId");
    String username = payload.get("username");

    Map<String, Object> response = new HashMap<>();

    response.put("type", "STOP_TYPING");
    response.put("chatId", chatId);
    response.put("username", username);

    messagingTemplate.convertAndSend(
            "/topic/chat/" + chatId,
            response
    );
}
}