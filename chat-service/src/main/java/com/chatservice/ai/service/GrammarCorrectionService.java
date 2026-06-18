package com.chatservice.ai.service;

import com.chatservice.ai.dto.AiDtos.GrammarCorrectionResponse;
import com.chatservice.ai.provider.AiProvider;
import com.chatservice.ai.provider.AiProviderRequest;
import com.chatservice.ai.provider.AiProviderResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GrammarCorrectionService {

    private static final String SYSTEM_INSTRUCTION_TEMPLATE =
            "You correct grammar and spelling in chat messages while strictly preserving the " +
            "original meaning and intent. Rewrite in a %s tone. Output ONLY the corrected text, " +
            "no explanation, no quotes.";

    private final AiProvider aiProvider;

    @Value("${gemini.model}")
    private String model;

    public GrammarCorrectionService(AiProvider aiProvider) {
        this.aiProvider = aiProvider;
    }

    public GrammarCorrectionResponse correct(String text, String tone) {
        String resolvedTone = "professional".equalsIgnoreCase(tone) ? "professional" : "casual";

        AiProviderRequest request = AiProviderRequest.builder()
                .systemInstruction(String.format(SYSTEM_INSTRUCTION_TEMPLATE, resolvedTone))
                .userMessage(text)
                .modelOverride(model)
                .temperature(0.4)
                .build();

        AiProviderResponse response = aiProvider.generate(request);
        return new GrammarCorrectionResponse(text, response.getText().trim());
    }
}