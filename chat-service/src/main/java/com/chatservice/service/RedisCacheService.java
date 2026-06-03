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

    private static final String MESSAGES_KEY_PREFIX  = "chat:messages:";
    private static final String ONLINE_KEY_PREFIX    = "presence:online:";
    private static final String LAST_SEEN_KEY_PREFIX = "presence:lastSeen:";

    // Online key TTL — if heartbeat doesn't refresh, user is considered offline
    private static final Duration ONLINE_TTL    = Duration.ofSeconds(70);
    // lastSeen is kept for 30 days
    private static final Duration LAST_SEEN_TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper        objectMapper;

    @Value("${app.cache.message-ttl-seconds:3600}")
    private long messageTtlSeconds;

    public RedisCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper  = objectMapper;
    }

    // ── Message cache ─────────────────────────────────────────────────────────

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

    // ── Presence ──────────────────────────────────────────────────────────────

    /**
     * Mark user as online in Redis with a TTL.
     * Call this on WebSocket connect (and optionally on heartbeat to refresh TTL).
     */
    public void setUserOnline(String username) {
        try {
            redisTemplate.opsForValue().set(
                ONLINE_KEY_PREFIX + username, "1", ONLINE_TTL
            );
            log.debug("Marked online: {}", username);
        } catch (Exception e) {
            log.warn("Redis setUserOnline failed for {}: {}", username, e.getMessage());
        }
    }

    /**
     * Mark user as offline and store their lastSeen timestamp (ISO-8601 string).
     * Call this on WebSocket disconnect.
     */
    public void setUserOffline(String username, String lastSeenIso) {
        try {
            // Remove the online key
            redisTemplate.delete(ONLINE_KEY_PREFIX + username);

            // Persist lastSeen
            redisTemplate.opsForValue().set(
                LAST_SEEN_KEY_PREFIX + username, lastSeenIso, LAST_SEEN_TTL
            );
            log.debug("Marked offline: {} lastSeen={}", username, lastSeenIso);
        } catch (Exception e) {
            log.warn("Redis setUserOffline failed for {}: {}", username, e.getMessage());
        }
    }

    /**
     * Returns true if the user currently has an active online key in Redis.
     */
    public boolean isUserOnline(String username) {
        try {
            return Boolean.TRUE.equals(
                redisTemplate.hasKey(ONLINE_KEY_PREFIX + username)
            );
        } catch (Exception e) {
            log.warn("Redis isUserOnline failed for {}: {}", username, e.getMessage());
            return false;
        }
    }

    /**
     * Returns the user's lastSeen ISO-8601 string, or null if never set.
     */
    public String getLastSeen(String username) {
        try {
            return redisTemplate.opsForValue().get(LAST_SEEN_KEY_PREFIX + username);
        } catch (Exception e) {
            log.warn("Redis getLastSeen failed for {}: {}", username, e.getMessage());
            return null;
        }
    }
}