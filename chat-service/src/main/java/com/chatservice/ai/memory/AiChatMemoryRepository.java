package com.chatservice.ai.memory;

import com.chatservice.ai.entity.AiMessage;
import com.chatservice.ai.repository.AiMessageRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AiChatMemoryRepository {

    private final AiMessageRepository aiMessageRepository;

    @Value("${ai.memory.max-history-messages:20}")
    private int maxHistoryMessages;

    public AiChatMemoryRepository(AiMessageRepository aiMessageRepository) {
        this.aiMessageRepository = aiMessageRepository;
    }

    /**
     * Returns up to maxHistoryMessages most recent messages for a conversation,
     * oldest first (correct order for feeding into provider history).
     */
    public List<AiMessage> loadRecentHistory(UUID conversationId) {
        List<AiMessage> all = aiMessageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
        if (all.size() <= maxHistoryMessages) {
            return all;
        }
        return all.subList(all.size() - maxHistoryMessages, all.size());
    }
}