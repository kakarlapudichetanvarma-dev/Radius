package com.chatservice.kafka;

import com.chatservice.entity.Message;
import com.chatservice.entity.Message.MessageStatus;
import com.chatservice.repository.MessageRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
public class ChatKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ChatKafkaConsumer.class);

    private final MessageRepository       messageRepository;
    private final SimpMessagingTemplate   messagingTemplate;
    private final ObjectMapper            objectMapper;

    public ChatKafkaConsumer(MessageRepository messageRepository,
                             SimpMessagingTemplate messagingTemplate,
                             ObjectMapper objectMapper) {
        this.messageRepository = messageRepository;
        this.messagingTemplate = messagingTemplate;
        this.objectMapper      = objectMapper;
    }

    @KafkaListener(topics = "${kafka.topic.message-delivered}",
                   groupId = "${spring.kafka.consumer.group-id}")
    public void onMessageDelivered(String payload) {
        try {
            Map<String, String> event =
                    objectMapper.readValue(payload, new TypeReference<>() {});
            String messageId = event.get("messageId");
            String chatId    = event.get("chatId");

            messageRepository.findById(UUID.fromString(messageId)).ifPresent(msg -> {
                if (msg.getStatus() == MessageStatus.SENT) {
                    msg.setStatus(MessageStatus.DELIVERED);
                    msg.setDeliveredAt(Instant.now());
                    messageRepository.save(msg);

                    // Push delivery status via WebSocket
                    messagingTemplate.convertAndSend(
                            "/topic/chat/" + chatId + "/status",
                            Map.of("messageId", messageId, "status", "DELIVERED")
                    );
                    log.info("Message {} marked DELIVERED in chat {}", messageId, chatId);
                }
            });
        } catch (Exception e) {
            log.error("Error processing MESSAGE_DELIVERED event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topic.message-read}",
                   groupId = "${spring.kafka.consumer.group-id}")
    public void onMessageRead(String payload) {
        try {
            Map<String, String> event =
                    objectMapper.readValue(payload, new TypeReference<>() {});
            String messageId = event.get("messageId");
            String chatId    = event.get("chatId");

            messageRepository.findById(UUID.fromString(messageId)).ifPresent(msg -> {
                msg.setStatus(MessageStatus.READ);
                msg.setReadAt(Instant.now());
                messageRepository.save(msg);

                messagingTemplate.convertAndSend(
                        "/topic/chat/" + chatId + "/status",
                        Map.of("messageId", messageId, "status", "READ")
                );
                log.info("Message {} marked READ in chat {}", messageId, chatId);
            });
        } catch (Exception e) {
            log.error("Error processing MESSAGE_READ event: {}", e.getMessage());
        }
    }

    @KafkaListener(topics = "${kafka.topic.chat-archived}",
                   groupId = "${spring.kafka.consumer.group-id}")
    public void onChatArchived(String payload) {
        try {
            Map<String, String> event =
                    objectMapper.readValue(payload, new TypeReference<>() {});
            log.info("Chat archived event received — chatId={} userId={}",
                    event.get("chatId"), event.get("userId"));
            // Notification-service would consume this topic separately
        } catch (Exception e) {
            log.error("Error processing CHAT_ARCHIVED event: {}", e.getMessage());
        }
    }
}
