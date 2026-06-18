package com.chatservice.ai;

import com.chatservice.ai.dto.AiDtos.*;

import java.util.UUID;

public interface AiService {

    /** General-purpose AI chat assistant. Maintains context via AiConversation. */
    AiChatResponse chat(UUID userId, AiChatRequest request);

    /** Coding assistant: explain errors, review code, generate examples, etc. */
    AiChatResponse codeAssist(UUID userId, CodeAssistRequest request);

    AiConversationHistoryResponse getHistory(UUID userId, String conversationType, String conversationId);
}