package com.chatservice.controller;

import com.chatservice.dto.ChatDtos.*;
import com.chatservice.entity.CallSession;
import com.chatservice.entity.CallSession.CallStatus;
import com.chatservice.repository.CallSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/chat/calls")
public class WebRtcController {

    private static final Logger log = LoggerFactory.getLogger(WebRtcController.class);

    private final CallSessionRepository callSessionRepository;

    public WebRtcController(CallSessionRepository callSessionRepository) {
        this.callSessionRepository = callSessionRepository;
    }

    // GET /calls/history — get all call history for logged-in user
    @GetMapping("/history")
    public ResponseEntity<ApiResponse> getCallHistory(Authentication auth) {
        UUID userId = UUID.fromString((String) auth.getPrincipal());
        log.info("GET /calls/history userId={}", userId);
        List<CallSessionResponse> history = callSessionRepository
                .findCallHistoryByUserId(userId)
                .stream()
                .map(this::toCallResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse(true, "Call history fetched.", history));
    }

    // GET /calls/chat/{chatId} — get calls in a specific chat
    // FIX: was findByChatIdOrderByStartedAtDesc — renamed to findByChatIdOrderByCreatedAtDesc
    @GetMapping("/chat/{chatId}")
    public ResponseEntity<ApiResponse> getChatCallHistory(
            @PathVariable UUID chatId,
            Authentication auth) {
        log.info("GET /calls/chat/{}", chatId);
        List<CallSessionResponse> calls = callSessionRepository
                .findByChatIdOrderByCreatedAtDesc(chatId)   // ← FIXED
                .stream()
                .map(this::toCallResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(new ApiResponse(true, "Chat call history fetched.", calls));
    }

    // GET /calls/{sessionId} — get specific call session
    @GetMapping("/{sessionId}")
    public ResponseEntity<ApiResponse> getCallSession(@PathVariable UUID sessionId) {
        CallSession session = callSessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Call session not found."));
        return ResponseEntity.ok(new ApiResponse(true, "Call session fetched.",
                toCallResponse(session)));
    }

    // GET /calls/chat/{chatId}/active — check if there is an active call
    // FIX: was findActiveCallInChat(chatId) — now takes List<CallStatus> param
    @GetMapping("/chat/{chatId}/active")
    public ResponseEntity<ApiResponse> getActiveCall(@PathVariable UUID chatId) {
        return callSessionRepository.findActiveCallInChat(
                        chatId,
                        List.of(CallStatus.INITIATED, CallStatus.RINGING, CallStatus.ACTIVE)) // ← FIXED
                .map(s -> ResponseEntity.ok(
                        new ApiResponse(true, "Active call found.", toCallResponse(s))))
                .orElse(ResponseEntity.ok(
                        new ApiResponse(true, "No active call.", null)));
    }

    private CallSessionResponse toCallResponse(CallSession s) {
        CallSessionResponse r = new CallSessionResponse();
        r.setSessionId(s.getId().toString());
        r.setChatId(s.getChatId().toString());
        r.setCallerId(s.getCallerId().toString());
        r.setCalleeId(s.getCalleeId().toString());
        r.setCallType(s.getCallType().name());
        r.setCallStatus(s.getCallStatus().name());
        r.setStartedAt(s.getStartedAt() != null ? s.getStartedAt().toString() : null);
        r.setAnsweredAt(s.getAnsweredAt() != null ? s.getAnsweredAt().toString() : null);
        r.setEndedAt(s.getEndedAt() != null ? s.getEndedAt().toString() : null);
        r.setDurationSeconds(s.getDurationSeconds());
        return r;
    }
}
