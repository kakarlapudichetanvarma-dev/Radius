package com.chatservice.ai;

import com.chatservice.ai.dto.AiDtos.*;
import com.chatservice.ai.entity.AiConversation;
import com.chatservice.ai.entity.AiConversation.ConversationType;
import com.chatservice.ai.entity.AiMessage;
import com.chatservice.ai.entity.AiMessage.Role;
import com.chatservice.ai.exception.AiExceptions.ConversationNotFoundException;
import com.chatservice.ai.memory.AiChatMemoryRepository;
import com.chatservice.ai.memory.CustomChatMemoryAdvisor;
import com.chatservice.ai.provider.AiProvider;
import com.chatservice.ai.provider.AiProviderRequest;
import com.chatservice.ai.provider.AiProviderResponse;
import com.chatservice.ai.repository.AiConversationRepository;
import com.chatservice.ai.repository.AiMessageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AiServiceImpl implements AiService {

    private static final Logger log = LoggerFactory.getLogger(AiServiceImpl.class);

    private static final String GENERAL_SYSTEM_INSTRUCTION =
            "You are a helpful, friendly AI assistant inside a chat application called Radius. " +
            "Answer questions clearly and concisely. Maintain awareness of the ongoing conversation.";

    private static final String CODING_SYSTEM_INSTRUCTION =
            "You are an expert Java, Spring Boot, React, and TypeScript assistant embedded in a " +
            "developer's chat application. You help explain Java exceptions and Spring Boot errors, " +
            "review code snippets, suggest fixes, generate code examples, and answer programming " +
            "questions. Be precise, use code blocks for code, and explain the root cause before the fix.";

    private final AiProvider aiProvider;
    private final AiConversationRepository conversationRepository;
    private final AiMessageRepository messageRepository;
    private final AiChatMemoryRepository memoryRepository;

    @Value("${gemini.model}")
    private String defaultModel;

    @Value("${gemini.coding-model}")
    private String codingModel;

    public AiServiceImpl(AiProvider aiProvider,
                          AiConversationRepository conversationRepository,
                          AiMessageRepository messageRepository,
                          AiChatMemoryRepository memoryRepository) {
        this.aiProvider = aiProvider;
        this.conversationRepository = conversationRepository;
        this.messageRepository = messageRepository;
        this.memoryRepository = memoryRepository;
    }

    @Override
    @Transactional
    public AiChatResponse chat(UUID userId, AiChatRequest request) {
        AiConversation conversation = resolveOrCreateConversation(
                userId, request.getConversationId(), ConversationType.GENERAL_CHAT);

        return generateAndPersist(conversation, request.getMessage(),
                GENERAL_SYSTEM_INSTRUCTION, defaultModel, "CHAT_REPLY");
    }

    @Override
    @Transactional
    public AiChatResponse codeAssist(UUID userId, CodeAssistRequest request) {
        AiConversation conversation = resolveOrCreateConversation(
                userId, request.getConversationId(), ConversationType.CODING_ASSISTANT);

        StringBuilder userTurn = new StringBuilder();
        userTurn.append(request.getPrompt());
        if (request.getLanguage() != null) {
            userTurn.append("\n\nLanguage/Stack: ").append(request.getLanguage());
        }
        if (request.getErrorMessage() != null && !request.getErrorMessage().isBlank()) {
            userTurn.append("\n\nError/Exception:\n```\n").append(request.getErrorMessage()).append("\n```");
        }
        if (request.getCode() != null && !request.getCode().isBlank()) {
            userTurn.append("\n\nCode:\n```\n").append(request.getCode()).append("\n```");
        }

        return generateAndPersist(conversation, userTurn.toString(),
                CODING_SYSTEM_INSTRUCTION, codingModel, "CODE_REPLY");
    }

    @Override
    public AiConversationHistoryResponse getHistory(UUID userId, String conversationTypeStr, String conversationId) {
        AiConversation conversation;

        if (conversationId != null) {
            conversation = conversationRepository.findById(UUID.fromString(conversationId))
                    .orElseThrow(() -> new ConversationNotFoundException("Conversation not found."));
            if (!conversation.getUserId().equals(userId)) {
                throw new ConversationNotFoundException("Conversation not found.");
            }
        } else {
            ConversationType type = ConversationType.valueOf(conversationTypeStr.toUpperCase());
            conversation = conversationRepository
                    .findFirstByUserIdAndConversationTypeOrderByUpdatedAtDesc(userId, type)
                    .orElseThrow(() -> new ConversationNotFoundException("No conversation found."));
        }

        List<AiMessage> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conversation.getId());

        AiConversationHistoryResponse response = new AiConversationHistoryResponse();
        response.setConversationId(conversation.getId().toString());
        response.setConversationType(conversation.getConversationType().name());
        response.setMessages(messages.stream().map(m -> {
            AiMessageItem item = new AiMessageItem();
            item.setRole(m.getRole().name());
            item.setContent(m.getContent());
            item.setSentAt(m.getSentAt().toString());
            return item;
        }).collect(Collectors.toList()));

        return response;
    }

    // ── internal helpers ─────────────────────────────────────────────────

    private AiConversation resolveOrCreateConversation(UUID userId, String conversationId, ConversationType type) {
        if (conversationId != null && !conversationId.isBlank()) {
            return conversationRepository.findById(UUID.fromString(conversationId))
                    .filter(c -> c.getUserId().equals(userId))
                    .orElseThrow(() -> new ConversationNotFoundException("Conversation not found."));
        }

        return conversationRepository
                .findFirstByUserIdAndConversationTypeOrderByUpdatedAtDesc(userId, type)
                .orElseGet(() -> {
                    AiConversation c = new AiConversation();
                    c.setUserId(userId);
                    c.setConversationType(type);
                    return conversationRepository.save(c);
                });
    }

    private AiChatResponse generateAndPersist(AiConversation conversation, String userMessage,
                                               String systemInstruction, String model, String responseType) {
        List<AiMessage> history = memoryRepository.loadRecentHistory(conversation.getId());

        AiProviderRequest providerRequest = AiProviderRequest.builder()
                .systemInstruction(systemInstruction)
                .history(CustomChatMemoryAdvisor.toProviderTurns(history))
                .userMessage(userMessage)
                .modelOverride(model)
                .build();

        AiProviderResponse providerResponse = aiProvider.generate(providerRequest);

        persistTurn(conversation.getId(), Role.USER, userMessage);
        persistTurn(conversation.getId(), Role.MODEL, providerResponse.getText());

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        AiChatResponse response = new AiChatResponse();
        response.setConversationId(conversation.getId().toString());
        response.setRole("MODEL");
        response.setContent(providerResponse.getText());
        response.setSentAt(Instant.now().toString());
        response.setType(responseType);
        return response;
    }

    private void persistTurn(UUID conversationId, Role role, String content) {
        AiMessage message = new AiMessage();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        messageRepository.save(message);
    }
}