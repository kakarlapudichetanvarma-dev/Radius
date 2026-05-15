package com.userservice.controller;

import com.userservice.dto.UserDtos.*;
import com.userservice.service.FriendService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;

@RestController
@RequestMapping("/friends")
public class FriendController {

    private static final Logger log =
            Logger.getLogger(
                    FriendController.class.getName()
            );

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/request")
    public ResponseEntity<ApiResponse> sendRequest(
            @Valid @RequestBody FriendRequestDto body,
            Authentication auth,
            @RequestHeader("Authorization") String token) {

        UUID requesterId =
                UUID.fromString(
                        (String) auth.getPrincipal()
                );

        FriendRequestResponse resp =
                friendService.sendFriendRequest(
                        requesterId,
                        body.getPhoneNumber(),
                        token
                );

        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage("Friend request sent successfully.");
        response.setData(resp);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/accept")
    public ResponseEntity<ApiResponse> respondRequest(
            @Valid @RequestBody FriendRequestActionDto body,
            Authentication auth,
            @RequestHeader("Authorization") String token) {

        UUID receiverId =
                UUID.fromString(
                        (String) auth.getPrincipal()
                );

        UUID requestId =
                UUID.fromString(body.getRequestId());

        boolean accept =
                "ACCEPT".equalsIgnoreCase(body.getAction());

        FriendRequestResponse resp =
                friendService.respondToRequest(
                        receiverId,
                        requestId,
                        accept,
                        token
                );

        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage(
                accept
                        ? "Friend request accepted."
                        : "Friend request rejected."
        );
        response.setData(resp);

        return ResponseEntity.ok(response);
    }

    // ── GET /friends/search ──────────────────────────────
    // Returns all friends of the logged-in user only
    @GetMapping("/search")
    public ResponseEntity<ApiResponse> listFriends(
            Authentication auth,
            @RequestHeader("Authorization") String token) {

        UUID loggedInUserId =
                UUID.fromString((String) auth.getPrincipal());

        List<FriendSummaryResponse> friends =
                friendService.listFriends(loggedInUserId, token);

        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage("Friends fetched successfully.");
        response.setData(friends);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/requests/pending")
    public ResponseEntity<ApiResponse> pendingRequests(
            Authentication auth,
            @RequestHeader("Authorization") String token) {

        UUID userId =
                UUID.fromString(
                        (String) auth.getPrincipal()
                );

        List<FriendRequestResponse> requests =
                friendService.listPendingRequests(userId, token);

        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage("Pending requests fetched.");
        response.setData(requests);

        return ResponseEntity.ok(response);
    }
}