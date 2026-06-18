package com.chatservice.ai;

import com.chatservice.ai.dto.AiDtos.*;
import com.chatservice.ai.service.GrammarCorrectionService;
import com.chatservice.ai.service.SmartReplyService;
import com.chatservice.ai.service.SummarizationService;
import com.chatservice.ai.service.TranslationService;
import com.chatservice.dto.ChatDtos.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/ai")
public class AiController {

    private final AiService aiService;
    private final SmartReplyService smartReplyService;
    private final TranslationService translationService;
    private final GrammarCorrectionService grammarCorrectionService;
    private final SummarizationService summarizationService;

    public AiController(AiService aiService,
                         SmartReplyService smartReplyService,
                         TranslationService translationService,
                         GrammarCorrectionService grammarCorrectionService,
                         SummarizationService summarizationService) {
        this.aiService = aiService;
        this.smartReplyService = smartReplyService;
        this.translationService = translationService;
        this.grammarCorrectionService = grammarCorrectionService;
        this.summarizationService = summarizationService;
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse> getHistory(
            @RequestParam(required = false) String conversationType,
            @RequestParam(required = false) String conversationId,
            Authentication auth) {
        UUID userId = uuid(auth);
        String type = conversationType != null ? conversationType : "GENERAL_CHAT";
        AiConversationHistoryResponse history = aiService.getHistory(userId, type, conversationId);
        return ResponseEntity.ok(new ApiResponse(true, "History fetched.", history));
    }
     @PostMapping("/chat")
    public ResponseEntity<ApiResponse> chat(
            @Valid @RequestBody AiChatRequest request, Authentication auth) {
        UUID userId = uuid(auth);
        AiChatResponse reply = aiService.chat(userId, request);
        return ResponseEntity.ok(new ApiResponse(true, "Reply generated.", reply));
    }

    @GetMapping("/smart-reply/{chatId}")
    public ResponseEntity<ApiResponse> getSmartReplies(
            @PathVariable UUID chatId, Authentication auth) {
        UUID userId = uuid(auth);
        SmartReplyResponse suggestions = smartReplyService.generateSuggestions(userId, chatId);
        return ResponseEntity.ok(new ApiResponse(true, "Suggestions generated.", suggestions));
    }

    @PostMapping("/translate")
    public ResponseEntity<ApiResponse> translate(
            @Valid @RequestBody TranslateRequest request, Authentication auth) {
        TranslateResponse result = translationService.translate(request.getText(), request.getTargetLanguage());
        return ResponseEntity.ok(new ApiResponse(true, "Translated.", result));
    }

    @PostMapping("/grammar-correct")
    public ResponseEntity<ApiResponse> correctGrammar(
            @Valid @RequestBody GrammarCorrectionRequest request, Authentication auth) {
        GrammarCorrectionResponse result = grammarCorrectionService.correct(request.getText(), request.getTone());
        return ResponseEntity.ok(new ApiResponse(true, "Grammar corrected.", result));
    }

    @PostMapping("/summarize")
    public ResponseEntity<ApiResponse> summarize(
            @Valid @RequestBody SummarizeRequest request, Authentication auth) {
        UUID userId = uuid(auth);
        SummarizeResponse result = summarizationService.summarize(
                userId, UUID.fromString(request.getChatId()), request.getMessageLimit());
        return ResponseEntity.ok(new ApiResponse(true, "Summary generated.", result));
    }

    private UUID uuid(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }
}