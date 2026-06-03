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
            Logger.getLogger(FriendController.class.getName());

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    // ── GET /friends/search ──────────────────────────────────────────────────
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

    // ── GET /friends/search-by-phone ─────────────────────────────────────────
    @GetMapping("/search-by-phone")
    public ResponseEntity<ApiResponse> searchByPhone(
            @RequestParam String phone,
            Authentication auth,
            @RequestHeader("Authorization") String token) {

        UUID requesterId =
                UUID.fromString((String) auth.getPrincipal());

        UserSearchResult result =
                friendService.searchByPhone(requesterId, phone, token);

        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage("User found.");
        response.setData(result);

        return ResponseEntity.ok(response);
    }

    // ── POST /friends/add-direct ─────────────────────────────────────────────
    @PostMapping("/add-direct")
    public ResponseEntity<ApiResponse> addDirectFriend(
            @Valid @RequestBody FriendRequestDto body,
            Authentication auth,
            @RequestHeader("Authorization") String token) {

        UUID requesterId =
                UUID.fromString((String) auth.getPrincipal());

        FriendSummaryResponse result =
                friendService.addDirectFriend(
                        requesterId,
                        body.getPhoneNumber(),
                        token
                );

        ApiResponse response = new ApiResponse();
        response.setSuccess(true);
        response.setMessage("Friend added successfully.");
        response.setData(result);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }
}