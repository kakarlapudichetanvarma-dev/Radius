package com.chatservice.ai.service;

import com.chatservice.ai.dto.AiDtos.SmartReplyResponse;
import com.chatservice.ai.exception.AiExceptions.AiProviderException;
import com.chatservice.ai.provider.AiProvider;
import com.chatservice.ai.provider.AiProviderRequest;
import com.chatservice.ai.provider.AiProviderResponse;
import com.chatservice.dto.ChatDtos.MessageResponse;
import com.chatservice.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SmartReplyService {

    private static final Logger log = LoggerFactory.getLogger(SmartReplyService.class);

    private static final String SYSTEM_INSTRUCTION =
            "You generate short, natural reply suggestions for a chat app, similar to Gmail's " +
            "Smart Reply. Given the most recent messages in a conversation, suggest exactly 3 " +
            "short replies (each under 8 words) the user might want to send next. " +
            "Output ONLY the 3 suggestions, one per line, no numbering, no quotes, no extra text.";

    private final ChatService chatService;
    private final AiProvider aiProvider;

    @Value("${gemini.model}")
    private String model;

    @Value("${ai.smart-reply.context-message-count:10}")
    private int contextMessageCount;

    public SmartReplyService(ChatService chatService, AiProvider aiProvider) {
        this.chatService = chatService;
        this.aiProvider = aiProvider;
    }

    public SmartReplyResponse generateSuggestions(UUID requestingUserId, UUID chatId) {
        List<MessageResponse> messages = chatService.getChatMessages(chatId, requestingUserId);

        if (messages.isEmpty()) {
            return new SmartReplyResponse(chatId.toString(), Collections.emptyList());
        }

        List<MessageResponse> recent = messages.size() > contextMessageCount
                ? messages.subList(messages.size() - contextMessageCount, messages.size())
                : messages;

        String transcript = recent.stream()
                .filter(m -> m.getContent() != null && !m.isDeleted())
                .map(m -> m.getSenderUsername() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        if (transcript.isBlank()) {
            return new SmartReplyResponse(chatId.toString(), Collections.emptyList());
        }

        try {
            AiProviderRequest request = AiProviderRequest.builder()
                    .systemInstruction(SYSTEM_INSTRUCTION)
                    .userMessage("Recent conversation:\n" + transcript + "\n\nSuggest 3 replies:")
                    .modelOverride(model)
                    .temperature(0.8)
                    .maxOutputTokens(150)
                    .build();

            AiProviderResponse response = aiProvider.generate(request);
            List<String> suggestions = parseSuggestions(response.getText());

            return new SmartReplyResponse(chatId.toString(), suggestions);

        } catch (AiProviderException e) {
            log.warn("Smart reply generation failed for chat {}: {}", chatId, e.getMessage());
            // Graceful degradation: empty suggestions, not an error surfaced to the user.
            // Smart replies are a nice-to-have; a Gemini hiccup shouldn't show an error banner.
            return new SmartReplyResponse(chatId.toString(), Collections.emptyList());
        }
    }

    private List<String> parseSuggestions(String text) {
        List<String> lines = new ArrayList<>();
        for (String line : text.split("\n")) {
            String cleaned = line.replaceAll("^[\\-\\*\\d\\.\\)\\s\"']+", "").trim();
            if (!cleaned.isBlank()) {
                lines.add(cleaned);
            }
            if (lines.size() == 3) break;
        }
        return lines;
    }
}