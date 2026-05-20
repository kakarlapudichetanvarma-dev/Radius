package com.chatservice.controller;

import com.chatservice.dto.ChatDtos.WsMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import java.security.Principal;

@Controller
public class ChatWebSocketController {

    private final SimpMessagingTemplate messagingTemplate;

    public ChatWebSocketController(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    // Client sends to /app/chat.send
    @MessageMapping("/chat.send")
    public void handleMessage(@Payload WsMessage message, Principal principal) {
        messagingTemplate.convertAndSend(
                "/topic/chat/" + message.getChatId(),
                message);
    }

    // Client sends to /app/chat.typing
    @MessageMapping("/chat.typing")
    public void handleTyping(@Payload String chatId, Principal principal) {
        if (principal != null) {
            messagingTemplate.convertAndSend(
                    "/topic/typing",
                    principal.getName());
        }
    }
}