package com.chatservice.ai.model;

/**
 * Identifies which AI provider is active. Currently only GEMINI is implemented.
 * Reserved for future multi-provider runtime switching.
 */
public enum AiProviderConfig {
    GEMINI,
    OPENAI,
    OLLAMA
}