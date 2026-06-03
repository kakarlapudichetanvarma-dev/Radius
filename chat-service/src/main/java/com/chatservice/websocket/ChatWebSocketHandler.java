package com.chatservice.websocket;

import com.chatservice.dto.ChatDtos.*;
import com.chatservice.kafka.ChatKafkaProducer;
import com.chatservice.service.RedisCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatKafkaProducer     kafkaProducer;
    private final RedisCacheService     cacheService;

    public ChatWebSocketHandler(SimpMessagingTemplate messagingTemplate,
                                ChatKafkaProducer kafkaProducer,
                                RedisCacheService cacheService) {
        this.messagingTemplate = messagingTemplate;
        this.kafkaProducer     = kafkaProducer;
        this.cacheService      = cacheService;
    }

    // ── Chat messages ─────────────────────────────────────────────────────────

    // Client sends to: /app/chat.send/{chatId}
    @MessageMapping("/chat.send/{chatId}")
    public void handleSend(
            @DestinationVariable String chatId,
            @Payload WsMessage message,
            Principal principal) {

        String senderId = principal != null ? principal.getName() : "unknown";
        log.info("WS MESSAGE from userId={} to chatId={} type={}",
                senderId, chatId, message.getMessageType());

        message.setChatId(chatId);
        message.setSenderId(senderId);
        message.setType("MESSAGE");

        messagingTemplate.convertAndSend("/topic/chat/" + chatId, message);

        if (message.getMessageId() != null) {
            kafkaProducer.publishMessageSent(chatId, message.getMessageId(), senderId);
        }
    }

    // Client sends to: /app/chat.delivered/{chatId}/{messageId}
    @MessageMapping("/chat.delivered/{chatId}/{messageId}")
    public void handleDelivered(
            @DestinationVariable String chatId,
            @DestinationVariable String messageId,
            Principal principal) {

        log.info("WS DELIVERED chatId={} messageId={}", chatId, messageId);
        kafkaProducer.publishMessageDelivered(chatId, messageId);
    }

    // Client sends to: /app/chat.read/{chatId}/{messageId}
    @MessageMapping("/chat.read/{chatId}/{messageId}")
    public void handleRead(
            @DestinationVariable String chatId,
            @DestinationVariable String messageId,
            Principal principal) {

        String readerId = principal != null ? principal.getName() : "unknown";
        log.info("WS READ chatId={} messageId={} readerId={}", chatId, messageId, readerId);
        kafkaProducer.publishMessageRead(chatId, messageId, readerId);
    }

    // Client sends to: /app/chat.typing/{chatId}
    @MessageMapping("/chat.typing/{chatId}")
    public void handleTyping(
            @DestinationVariable String chatId,
            Principal principal) {

        String userId = principal != null ? principal.getName() : "unknown";
        log.debug("WS TYPING chatId={} userId={}", chatId, userId);

        WsMessage typing = new WsMessage();
        typing.setType("TYPING");
        typing.setChatId(chatId);
        typing.setSenderId(userId);

        messagingTemplate.convertAndSend("/topic/chat/" + chatId + "/typing", typing);
    }

    // ── Presence heartbeat ────────────────────────────────────────────────────

    /**
     * Client sends to: /app/presence.heartbeat every 30s.
     * Refreshes the Redis online TTL so the user stays "online"
     * as long as their browser tab is open and active.
     */
    @MessageMapping("/presence.heartbeat")
    public void handleHeartbeat(Principal principal) {
        if (principal == null) return;

        String username = principal.getName();
        cacheService.setUserOnline(username); // resets the 70s TTL
        log.debug("HEARTBEAT username={}", username);
    }
}