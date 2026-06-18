package com.chatservice.ai.provider;

/**
 * Provider-agnostic contract for any LLM backend (Gemini, OpenAI, Ollama, ...).
 * AiServiceImpl and all feature services depend ONLY on this interface.
 * Swapping providers = writing a new implementation + changing one @Bean
 * in AiConfig. No other file in the ai/ package needs to change.
 */
public interface AiProvider {

    AiProviderResponse generate(AiProviderRequest request);

    String providerName();
}