package com.chatservice.controller;

import com.chatservice.dto.ChatDtos.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Returns ICE server config (STUN/TURN) to the frontend.
 * The frontend passes these to RTCPeerConnection({ iceServers: [...] })
 */
@RestController
@RequestMapping("/api/v1/chat/webrtc")
public class IceServerController {

    @Value("${webrtc.stun.url:stun:stun.l.google.com:19302}")
    private String stunUrl;

    @Value("${webrtc.turn.url:}")
    private String turnUrl;

    @Value("${webrtc.turn.username:}")
    private String turnUsername;

    @Value("${webrtc.turn.credential:}")
    private String turnCredential;

    // GET /webrtc/ice-servers
    // Frontend calls this to get ICE server list before starting a call
    @GetMapping("/ice-servers")
    public ResponseEntity<ApiResponse> getIceServers() {
        List<Map<String, Object>> iceServers = new ArrayList<>();

        // Always add Google STUN
        Map<String, Object> stun = new HashMap<>();
        stun.put("urls", stunUrl);
        iceServers.add(stun);

        // Add TURN if configured
        if (turnUrl != null && !turnUrl.isBlank()) {
            Map<String, Object> turn = new HashMap<>();
            turn.put("urls", turnUrl);
            turn.put("username", turnUsername);
            turn.put("credential", turnCredential);
            iceServers.add(turn);
        }

        return ResponseEntity.ok(new ApiResponse(true, "ICE servers fetched.", iceServers));
    }
}
