package com.chatservice.ai.command;

import com.chatservice.ai.AiService;
import com.chatservice.ai.command.AiCommandParser.ParsedCommand;
import com.chatservice.ai.dto.AiDtos.*;
import com.chatservice.ai.service.SummarizationService;
import com.chatservice.ai.tool.GroupSendTool;
import com.chatservice.config.UserServiceClient;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AiCommandService {

    private final AiCommandParser commandParser;
    private final AiService aiService;
    private final GroupSendTool groupSendTool;
    private final UserServiceClient userServiceClient;
    private final SummarizationService summarizationService;

    public AiCommandService(AiCommandParser commandParser,
                             AiService aiService,
                             GroupSendTool groupSendTool,
                             UserServiceClient userServiceClient,
                             SummarizationService summarizationService) {
        this.commandParser = commandParser;
        this.aiService = aiService;
        this.groupSendTool = groupSendTool;
        this.userServiceClient = userServiceClient;
        this.summarizationService = summarizationService;
    }

    public AiChatResponse handle(UUID userId, String rawText, String conversationId, String contextChatId) {
        ParsedCommand parsed = commandParser.parse(rawText);

        switch (parsed.getType()) {
            case GROUP_SEND:
                return toGroupSendResponse(handleGroupSend(userId, parsed), conversationId);

            case SUMMARIZE:
                if (contextChatId == null) {
                    return errorResponse(conversationId, "Open a chat first, then ask me to summarize it.");
                }
                SummarizeResponse summary = summarizationService.summarize(
                        userId, UUID.fromString(contextChatId), null);
                return toSummaryResponse(summary, conversationId);

            // TRANSLATE and GRAMMAR_FIX detected here are intentionally NOT
            // auto-actioned, because neither carries an unambiguous target
            // (which message?) from free text alone. They fall through to
            // plain chat, where Gemini responds conversationally; the
            // dedicated one-shot REST endpoints (/translate, /grammar-correct)
            // are the real UX for these two and always pass explicit text.
            case TRANSLATE:
            case GRAMMAR_FIX:
            case PLAIN_CHAT:
            default:
                return plainChatFallback(userId, rawText, conversationId);
        }
    }

    private AiChatResponse plainChatFallback(UUID userId, String rawText, String conversationId) {
        AiChatRequest request = new AiChatRequest();
        request.setMessage(rawText);
        request.setConversationId(conversationId);
        return aiService.chat(userId, request);
    }

    private GroupCommandResult handleGroupSend(UUID userId, ParsedCommand parsed) {
        String senderUsername = userServiceClient.getUsernameById(userId);
        return groupSendTool.sendToGroup(userId, senderUsername, parsed.getGroupName(), parsed.getMessageContent());
    }

    private AiChatResponse toGroupSendResponse(GroupCommandResult result, String conversationId) {
        AiChatResponse response = new AiChatResponse();
        response.setConversationId(conversationId);
        response.setRole("MODEL");
        response.setType(result.isSuccess() ? "GROUP_SEND_CONFIRMATION" : "ERROR");
        response.setContent(result.getMessage());
        return response;
    }

    private AiChatResponse toSummaryResponse(SummarizeResponse summary, String conversationId) {
        AiChatResponse response = new AiChatResponse();
        response.setConversationId(conversationId);
        response.setRole("MODEL");
        response.setType("SUMMARY");
        response.setContent(summary.getSummary());
        return response;
    }

    private AiChatResponse errorResponse(String conversationId, String message) {
        AiChatResponse response = new AiChatResponse();
        response.setConversationId(conversationId);
        response.setType("ERROR");
        response.setContent(message);
        return response;
    }
}