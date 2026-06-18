package com.chatservice.ai.service;

import com.chatservice.ai.dto.AiDtos.TranslateResponse;
import com.chatservice.ai.provider.AiProvider;
import com.chatservice.ai.provider.AiProviderRequest;
import com.chatservice.ai.provider.AiProviderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TranslationService {

    private static final String SYSTEM_INSTRUCTION =
            "You are a translation engine. Translate the given text into the requested target " +
            "language. Preserve tone, formatting, and emoji. Output ONLY the translated text, " +
            "with no explanation, no quotes, and no language label.";

    private final AiProvider aiProvider;

    @Value("${gemini.model}")
    private String model;

    public TranslationService(AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    public TranslateResponse translate(String text, String targetLanguage) {
        AiProviderRequest request = AiProviderRequest.builder()
                .systemInstruction(SYSTEM_INSTRUCTION)
                .userMessage("Target language: " + targetLanguage + "\n\nText:\n" + text)
                .modelOverride(model)
                .temperature(0.3)
                .build();

        AiProviderResponse response = aiProvider.generate(request);
        return new TranslateResponse(text, response.getText().trim(), targetLanguage);
    }
}