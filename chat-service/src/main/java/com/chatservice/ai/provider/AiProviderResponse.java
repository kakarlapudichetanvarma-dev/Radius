package com.chatservice.ai.provider;

public class AiProviderResponse {

    private final String text;
    private final boolean truncated;
    private final Integer promptTokens;
    private final Integer responseTokens;

    public AiProviderResponse(String text, boolean truncated, Integer promptTokens, Integer responseTokens) {
        this.text = text;
        this.truncated = truncated;
        this.promptTokens = promptTokens;
        this.responseTokens = responseTokens;
    }

    public String getText() { return text; }
    public boolean isTruncated() { return truncated; }
    public Integer getPromptTokens() { return promptTokens; }
    public Integer getResponseTokens() { return responseTokens; }
}