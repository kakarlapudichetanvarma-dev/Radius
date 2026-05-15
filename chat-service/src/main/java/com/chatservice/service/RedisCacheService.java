package com.chatservice.service;

import com.chatservice.dto.ChatDtos.MessageResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;

@Service
public class RedisCacheService {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheService.class);

    private static final String MESSAGES_KEY_PREFIX = "chat:messages:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    @Value("${app.cache.message-ttl-seconds:3600}")
    private long messageTtlSeconds;

    public RedisCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper  = objectMapper;
    }

    public List<MessageResponse> getCachedMessages(String chatId) {
        try {
            String key   = MESSAGES_KEY_PREFIX + chatId;
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) return null;
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("Redis GET failed for chatId={}: {}", chatId, e.getMessage());
            return null;
        }
    }

    public void cacheMessages(String chatId, List<MessageResponse> messages) {
        try {
            String key   = MESSAGES_KEY_PREFIX + chatId;
            String value = objectMapper.writeValueAsString(messages);
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(messageTtlSeconds));
            log.debug("Cached {} messages for chatId={}", messages.size(), chatId);
        } catch (Exception e) {
            log.warn("Redis SET failed for chatId={}: {}", chatId, e.getMessage());
        }
    }

    public void evictChatMessages(String chatId) {
        try {
            redisTemplate.delete(MESSAGES_KEY_PREFIX + chatId);
            log.debug("Evicted cache for chatId={}", chatId);
        } catch (Exception e) {
            log.warn("Redis DELETE failed for chatId={}: {}", chatId, e.getMessage());
        }
    }
}
