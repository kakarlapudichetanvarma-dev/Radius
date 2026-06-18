package com.chatservice.ai.provider;

import com.chatservice.ai.config.GeminiProperties;
import com.chatservice.ai.exception.AiExceptions.AiProviderException;
import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Gemini SDK implementation of AiProvider. This is the ONLY class in the
 * entire ai/ package that imports com.google.genai.*. If you ever swap
 * providers, this is the only file you delete/replace.
 */
@Component
public class GeminiAiProvider implements AiProvider {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiProvider.class);

    private final Client client;
    private final GeminiProperties properties;

    public GeminiAiProvider(Client client, GeminiProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public AiProviderResponse generate(AiProviderRequest request) {
        try {
            List<Content> contents = new ArrayList<>();

            for (AiProviderRequest.Turn turn : request.getHistory()) {
                String role = "model".equals(turn.getRole()) ? "model" : "user";
                contents.add(Content.builder()
                        .role(role)
                        .parts(List.of(Part.fromText(turn.getText())))
                        .build());
            }

            contents.add(Content.builder()
                    .role("user")
                    .parts(List.of(Part.fromText(request.getUserMessage())))
                    .build());

            GenerateContentConfig.Builder configBuilder = GenerateContentConfig.builder()
                    .temperature(request.getTemperature() != null
                            ? request.getTemperature().floatValue()
                            : properties.getTemperature())
                    .maxOutputTokens(request.getMaxOutputTokens() != null
                            ? request.getMaxOutputTokens()
                            : properties.getMaxOutputTokens());

            if (request.getSystemInstruction() != null) {
                configBuilder.systemInstruction(Content.builder()
                        .parts(List.of(Part.fromText(request.getSystemInstruction())))
                        .build());
            }

            String model = request.getModelOverride() != null
                    ? request.getModelOverride()
                    : properties.getModel();

            GenerateContentResponse response = client.models.generateContent(
                    model, contents, configBuilder.build());

            String text = response.text();
            if (text == null || text.isBlank()) {
                throw new AiProviderException("Gemini returned an empty response.");
            }

            return new AiProviderResponse(text, false, null, null);

        } catch (AiProviderException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini API call failed: {}", e.getMessage(), e);
            throw new AiProviderException("AI provider request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public String providerName() {
        return "gemini";
    }
}