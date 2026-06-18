package com.chatservice.ai.service;

import com.chatservice.ai.dto.AiDtos.SummarizeResponse;
import com.chatservice.ai.provider.AiProvider;
import com.chatservice.ai.provider.AiProviderRequest;
import com.chatservice.ai.provider.AiProviderResponse;
import com.chatservice.dto.ChatDtos.MessageResponse;
import com.chatservice.service.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SummarizationService {

    private static final String SYSTEM_INSTRUCTION =
            "You summarize chat conversations concisely. Produce a short summary (3-6 sentences " +
            "or bullet points) covering the main topics discussed, any decisions made, and any " +
            "action items mentioned. Do not invent details not present in the transcript.";

    private final ChatService chatService;
    private final AiProvider aiProvider;

    @Value("${gemini.model}")
    private String model;

    public SummarizationService(ChatService chatService, AiProvider aiProvider) {
        this.chatService = chatService;
        this.aiProvider = aiProvider;
    }

    public SummarizeResponse summarize(UUID requestingUserId, UUID chatId, Integer messageLimit) {
        List<MessageResponse> messages = chatService.getChatMessages(chatId, requestingUserId);

        if (messages.isEmpty()) {
            return new SummarizeResponse(chatId.toString(), "There are no messages to summarize yet.");
        }

        List<MessageResponse> scoped = (messageLimit != null && messages.size() > messageLimit)
                ? messages.subList(messages.size() - messageLimit, messages.size())
                : messages;

        String transcript = scoped.stream()
                .filter(m -> m.getContent() != null && !m.isDeleted())
                .map(m -> m.getSenderUsername() + ": " + m.getContent())
                .collect(Collectors.joining("\n"));

        if (transcript.isBlank()) {
            return new SummarizeResponse(chatId.toString(), "There's nothing substantive to summarize yet.");
        }

        AiProviderRequest request = AiProviderRequest.builder()
                .systemInstruction(SYSTEM_INSTRUCTION)
                .userMessage("Conversation transcript:\n" + transcript)
                .modelOverride(model)
                .temperature(0.3)
                .maxOutputTokens(500)
                .build();

        AiProviderResponse response = aiProvider.generate(request);
        return new SummarizeResponse(chatId.toString(), response.getText().trim());
    }
}