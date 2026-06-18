package com.chatservice.ai;

import com.chatservice.ai.command.AiCommandService;
import com.chatservice.ai.dto.AiDtos.AiChatRequest;
import com.chatservice.ai.dto.AiDtos.AiChatResponse;
import com.chatservice.ai.dto.AiDtos.CodeAssistRequest;
import com.chatservice.config.UserServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.UUID;

@Controller
public class AiWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(AiWebSocketHandler.class);

    private final AiService aiService;
    private final AiCommandService aiCommandService;
    private final SimpMessagingTemplate messagingTemplate;
    private final UserServiceClient userServiceClient;

    public AiWebSocketHandler(AiService aiService,
                               AiCommandService aiCommandService,
                               SimpMessagingTemplate messagingTemplate,
                               UserServiceClient userServiceClient) {
        this.aiService = aiService;
        this.aiCommandService = aiCommandService;
        this.messagingTemplate = messagingTemplate;
        this.userServiceClient = userServiceClient;
    }

    @MessageMapping("/ai/chat")
    public void handleChat(@Payload AiChatRequest request, Principal principal) {
        String username = principal.getName();
        UUID userId = userServiceClient.getUserIdByUsername(username);
        try {
            AiChatResponse response = aiCommandService.handle(
                    userId, request.getMessage(), request.getConversationId(), request.getContextChatId());
            // NOTE: convertAndSendToUser keys on the STOMP Principal's name,
            // which is the USERNAME here, not the userId. Must send to
            // username, mirroring how WebSocketConfig populated the principal.
            messagingTemplate.convertAndSendToUser(username, "/queue/ai", response);
        } catch (Exception e) {
            log.error("AI chat failed for user {}: {}", username, e.getMessage(), e);
            sendError(username, "Sorry, something went wrong processing that request.");
        }
    }

    @MessageMapping("/ai/code")
    public void handleCodeAssist(@Payload CodeAssistRequest request, Principal principal) {
        String username = principal.getName();
        UUID userId = userServiceClient.getUserIdByUsername(username);
        try {
            AiChatResponse response = aiService.codeAssist(userId, request);
            messagingTemplate.convertAndSendToUser(username, "/queue/ai", response);
        } catch (Exception e) {
            log.error("Code assist failed for user {}: {}", username, e.getMessage(), e);
            sendError(username, "Sorry, I couldn't process that code/error right now.");
        }
    }

    private void sendError(String username, String message) {
        AiChatResponse error = new AiChatResponse();
        error.setType("ERROR");
        error.setContent(message);
        messagingTemplate.convertAndSendToUser(username, "/queue/ai", error);
    }
}