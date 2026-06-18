package com.chatservice.ai.provider;

import java.util.Collections;
import java.util.List;

/**
 * Provider-agnostic request model. No Gemini-specific types leak past this class.
 */
public class AiProviderRequest {

    /** One turn of conversation history, oldest first. */
    public static class Turn {
        private final String role;   // "user" or "model"
        private final String text;

        public Turn(String role, String text) {
            this.role = role;
            this.text = text;
        }
        public String getRole() { return role; }
        public String getText() { return text; }
    }

    private final String systemInstruction;
    private final List<Turn> history;
    private final String userMessage;
    private final Double temperature;
    private final Integer maxOutputTokens;
    private final String modelOverride; // null = use default model from config

    private AiProviderRequest(Builder b) {
        this.systemInstruction = b.systemInstruction;
        this.history = b.history;
        this.userMessage = b.userMessage;
        this.temperature = b.temperature;
        this.maxOutputTokens = b.maxOutputTokens;
        this.modelOverride = b.modelOverride;
    }

    public String getSystemInstruction() { return systemInstruction; }
    public List<Turn> getHistory() { return history; }
    public String getUserMessage() { return userMessage; }
    public Double getTemperature() { return temperature; }
    public Integer getMaxOutputTokens() { return maxOutputTokens; }
    public String getModelOverride() { return modelOverride; }

    public static Builder builder() { return new Builder(); }

    public static class Builder {
        private String systemInstruction;
        private List<Turn> history = Collections.emptyList();
        private String userMessage;
        private Double temperature;
        private Integer maxOutputTokens;
        private String modelOverride;

        public Builder systemInstruction(String v) { this.systemInstruction = v; return this; }
        public Builder history(List<Turn> v) { this.history = v; return this; }
        public Builder userMessage(String v) { this.userMessage = v; return this; }
        public Builder temperature(Double v) { this.temperature = v; return this; }
        public Builder maxOutputTokens(Integer v) { this.maxOutputTokens = v; return this; }
        public Builder modelOverride(String v) { this.modelOverride = v; return this; }
        public AiProviderRequest build() { return new AiProviderRequest(this); }
    }
}