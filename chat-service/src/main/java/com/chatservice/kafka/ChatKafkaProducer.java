package com.chatservice.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ChatKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(ChatKafkaProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${kafka.topic.message-sent}")
    private String messageSentTopic;

    @Value("${kafka.topic.message-delivered}")
    private String messageDeliveredTopic;

    @Value("${kafka.topic.message-read}")
    private String messageReadTopic;

    @Value("${kafka.topic.chat-archived}")
    private String chatArchivedTopic;

    @Value("${kafka.topic.group-event}")
    private String groupEventTopic;

    public ChatKafkaProducer(KafkaTemplate<String, String> kafkaTemplate,
                             ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper  = objectMapper;
    }

    public void publishMessageSent(String chatId, String messageId, String senderId) {
        send(messageSentTopic, chatId, Map.of(
                "event",     "MESSAGE_SENT",
                "chatId",    chatId,
                "messageId", messageId,
                "senderId",  senderId
        ));
    }

    public void publishMessageDelivered(String chatId, String messageId) {
        send(messageDeliveredTopic, chatId, Map.of(
                "event",     "MESSAGE_DELIVERED",
                "chatId",    chatId,
                "messageId", messageId
        ));
    }

    public void publishMessageRead(String chatId, String messageId, String readerId) {
        send(messageReadTopic, chatId, Map.of(
                "event",     "MESSAGE_READ",
                "chatId",    chatId,
                "messageId", messageId,
                "readerId",  readerId
        ));
    }

    public void publishChatArchived(String chatId, String userId) {
        send(chatArchivedTopic, chatId, Map.of(
                "event",  "CHAT_ARCHIVED",
                "chatId", chatId,
                "userId", userId
        ));
    }

    public void publishGroupEvent(String groupId, String eventType,
                                   String actorId, String description) {
        send(groupEventTopic, groupId, Map.of(
                "event",       "GROUP_EVENT",
                "groupId",     groupId,
                "eventType",   eventType,
                "actorId",     actorId,
                "description", description
        ));
    }

    private void send(String topic, String key, Map<String, String> payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(topic, key, json);
            log.debug("Kafka → topic={} key={} payload={}", topic, key, json);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Kafka payload for topic {}: {}", topic, e.getMessage());
        }
    }
}
