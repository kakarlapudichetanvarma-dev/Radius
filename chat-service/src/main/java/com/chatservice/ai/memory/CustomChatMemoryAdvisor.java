package com.chatservice.ai.memory;

import com.chatservice.ai.entity.AiMessage;
import com.chatservice.ai.provider.AiProviderRequest;

import java.util.List;
import java.util.stream.Collectors;

public class CustomChatMemoryAdvisor {

    public static List<AiProviderRequest.Turn> toProviderTurns(List<AiMessage> messages) {
        return messages.stream()
                .map(m -> new AiProviderRequest.Turn(
                        m.getRole() == AiMessage.Role.MODEL ? "model" : "user",
                        m.getContent()))
                .collect(Collectors.toList());
    }
}