package com.chatservice.websocket;

import com.chatservice.dto.ChatDtos.*;
import com.chatservice.entity.CallSession;
import com.chatservice.entity.CallSession.CallStatus;
import com.chatservice.entity.CallSession.CallType;
import com.chatservice.repository.CallSessionRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

/**
 * WebRTC Signaling Server over STOMP WebSocket.
 *
 * Flow:
 *  1. Caller  → /app/call.initiate/{chatId}        (send offer + callType)
 *  2. Server  → /topic/call/{chatId}                (broadcast CALL_INITIATED to callee)
 *  3. Callee  → /app/call.answer/{sessionId}        (send answer SDP)
 *  4. Server  → /topic/call/{chatId}                (broadcast CALL_ANSWERED to caller)
 *  5. Both    → /app/call.ice/{sessionId}           (exchange ICE candidates)
 *  6. Server  → /topic/call/{chatId}                (relay ICE to other peer)
 *  7. Either  → /app/call.end/{sessionId}           (hang up)
 *  8. Server  → /topic/call/{chatId}                (broadcast CALL_ENDED)
 */
@Controller
public class WebRtcWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(WebRtcWebSocketHandler.class);

    private final SimpMessagingTemplate messagingTemplate;
    private final CallSessionRepository callSessionRepository;
    private final ObjectMapper          objectMapper;

    public WebRtcWebSocketHandler(
            SimpMessagingTemplate messagingTemplate,
            CallSessionRepository callSessionRepository,
            ObjectMapper objectMapper) {
        this.messagingTemplate    = messagingTemplate;
        this.callSessionRepository = callSessionRepository;
        this.objectMapper         = objectMapper;
    }

    // ── Step 1: Caller initiates a call ──────────────────────────────────────
    // Client sends: /app/call.initiate/{chatId}
    // Payload: { calleeId, callType: "AUDIO"|"VIDEO", sdpOffer }
    @MessageMapping("/call.initiate/{chatId}")
    public void initiateCall(
            @DestinationVariable String chatId,
            @Payload WebRtcSignal signal,
            Principal principal) {

        String callerId = principal.getName();
        log.info("WebRTC INITIATE chatId={} callerId={} type={}", chatId, callerId, signal.getCallType());

        // Persist call session
        CallSession session = new CallSession();
        session.setChatId(UUID.fromString(chatId));
        session.setCallerId(UUID.fromString(callerId));
        session.setCalleeId(UUID.fromString(signal.getCalleeId()));
        session.setCallType(CallType.valueOf(signal.getCallType().toUpperCase()));
        session.setCallStatus(CallStatus.RINGING);
        session = callSessionRepository.save(session);

        // Notify callee
        WebRtcSignal outgoing = new WebRtcSignal();
        outgoing.setType("CALL_INITIATED");
        outgoing.setSessionId(session.getId().toString());
        outgoing.setChatId(chatId);
        outgoing.setCallerId(callerId);
        outgoing.setCalleeId(signal.getCalleeId());
        outgoing.setCallType(signal.getCallType());
        outgoing.setSdp(signal.getSdp()); // SDP offer

        messagingTemplate.convertAndSend("/topic/call/" + chatId, outgoing);
        log.info("WebRTC CALL_INITIATED sessionId={}", session.getId());
    }

    // ── Step 2: Callee answers ────────────────────────────────────────────────
    // Client sends: /app/call.answer/{sessionId}
    // Payload: { sdpAnswer }
    @MessageMapping("/call.answer/{sessionId}")
    public void answerCall(
            @DestinationVariable String sessionId,
            @Payload WebRtcSignal signal,
            Principal principal) {

        String calleeId = principal.getName();
        log.info("WebRTC ANSWER sessionId={} calleeId={}", sessionId, calleeId);

        callSessionRepository.findById(UUID.fromString(sessionId)).ifPresent(session -> {
            session.setCallStatus(CallStatus.ACTIVE);
            session.setAnsweredAt(Instant.now());
            callSessionRepository.save(session);

            WebRtcSignal outgoing = new WebRtcSignal();
            outgoing.setType("CALL_ANSWERED");
            outgoing.setSessionId(sessionId);
            outgoing.setChatId(session.getChatId().toString());
            outgoing.setCallerId(session.getCallerId().toString());
            outgoing.setCalleeId(calleeId);
            outgoing.setSdp(signal.getSdp()); // SDP answer

            // Send answer back to caller
            messagingTemplate.convertAndSend(
                    "/topic/call/" + session.getChatId(), outgoing);
            log.info("WebRTC CALL_ANSWERED sessionId={}", sessionId);
        });
    }

    // ── Step 3: Decline call ──────────────────────────────────────────────────
    // Client sends: /app/call.decline/{sessionId}
    @MessageMapping("/call.decline/{sessionId}")
    public void declineCall(
            @DestinationVariable String sessionId,
            Principal principal) {

        log.info("WebRTC DECLINE sessionId={}", sessionId);

        callSessionRepository.findById(UUID.fromString(sessionId)).ifPresent(session -> {
            session.setCallStatus(CallStatus.DECLINED);
            session.setEndedAt(Instant.now());
            callSessionRepository.save(session);

            WebRtcSignal outgoing = new WebRtcSignal();
            outgoing.setType("CALL_DECLINED");
            outgoing.setSessionId(sessionId);
            outgoing.setChatId(session.getChatId().toString());
            outgoing.setCallerId(session.getCallerId().toString());
            outgoing.setCalleeId(session.getCalleeId().toString());

            messagingTemplate.convertAndSend(
                    "/topic/call/" + session.getChatId(), outgoing);
        });
    }

    // ── Step 4: ICE candidate exchange ───────────────────────────────────────
    // Client sends: /app/call.ice/{sessionId}
    // Payload: { candidate, sdpMid, sdpMLineIndex }
    @MessageMapping("/call.ice/{sessionId}")
    public void exchangeIce(
            @DestinationVariable String sessionId,
            @Payload WebRtcSignal signal,
            Principal principal) {

        String senderId = principal.getName();
        log.debug("WebRTC ICE sessionId={} from={}", sessionId, senderId);

        callSessionRepository.findById(UUID.fromString(sessionId)).ifPresent(session -> {
            WebRtcSignal outgoing = new WebRtcSignal();
            outgoing.setType("ICE_CANDIDATE");
            outgoing.setSessionId(sessionId);
            outgoing.setChatId(session.getChatId().toString());
            outgoing.setFromUserId(senderId);
            outgoing.setCandidate(signal.getCandidate());
            outgoing.setSdpMid(signal.getSdpMid());
            outgoing.setSdpMLineIndex(signal.getSdpMLineIndex());

            // Relay ICE to the other peer
            messagingTemplate.convertAndSend(
                    "/topic/call/" + session.getChatId(), outgoing);
        });
    }

    // ── Step 5: End call ─────────────────────────────────────────────────────
    // Client sends: /app/call.end/{sessionId}
    @MessageMapping("/call.end/{sessionId}")
    public void endCall(
            @DestinationVariable String sessionId,
            Principal principal) {

        String userId = principal.getName();
        log.info("WebRTC END sessionId={} by={}", sessionId, userId);

        callSessionRepository.findById(UUID.fromString(sessionId)).ifPresent(session -> {
            session.setCallStatus(CallStatus.ENDED);
            session.setEndedAt(Instant.now());

            if (session.getAnsweredAt() != null) {
                long seconds = Instant.now().getEpochSecond()
                        - session.getAnsweredAt().getEpochSecond();
                session.setDurationSeconds(seconds);
            }

            callSessionRepository.save(session);

            WebRtcSignal outgoing = new WebRtcSignal();
            outgoing.setType("CALL_ENDED");
            outgoing.setSessionId(sessionId);
            outgoing.setChatId(session.getChatId().toString());
            outgoing.setCallerId(session.getCallerId().toString());
            outgoing.setCalleeId(session.getCalleeId().toString());
            outgoing.setDurationSeconds(session.getDurationSeconds());

            messagingTemplate.convertAndSend(
                    "/topic/call/" + session.getChatId(), outgoing);
            log.info("WebRTC CALL_ENDED sessionId={} duration={}s",
                    sessionId, session.getDurationSeconds());
        });
    }

    // ── Missed call (timeout from caller side) ───────────────────────────────
    // Client sends: /app/call.missed/{sessionId}
    @MessageMapping("/call.missed/{sessionId}")
    public void missedCall(
            @DestinationVariable String sessionId,
            Principal principal) {

        log.info("WebRTC MISSED sessionId={}", sessionId);

        callSessionRepository.findById(UUID.fromString(sessionId)).ifPresent(session -> {
            if (session.getCallStatus() == CallStatus.RINGING) {
                session.setCallStatus(CallStatus.MISSED);
                session.setEndedAt(Instant.now());
                callSessionRepository.save(session);

                WebRtcSignal outgoing = new WebRtcSignal();
                outgoing.setType("CALL_MISSED");
                outgoing.setSessionId(sessionId);
                outgoing.setChatId(session.getChatId().toString());
                outgoing.setCalleeId(session.getCalleeId().toString());

                messagingTemplate.convertAndSend(
                        "/topic/call/" + session.getChatId(), outgoing);
            }
        });
    }
}
