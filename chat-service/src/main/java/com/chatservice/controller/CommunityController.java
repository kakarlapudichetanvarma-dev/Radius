package com.chatservice.controller;

import com.chatservice.dto.CommunityDtos.*;
import com.chatservice.security.JwtUtil;
import com.chatservice.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final JwtUtil jwtUtil;

    private UUID userId(String authHeader) {
        return UUID.fromString(jwtUtil.extractUserId(authHeader.replace("Bearer ", "")));
    }

    // ── Community CRUD ────────────────────────────────────────────────────────

    @PostMapping
    public ResponseEntity<CommunityResponse> createCommunity(
            @RequestHeader("Authorization") String token,
            @RequestBody CreateCommunityRequest request) {
        return ResponseEntity.ok(communityService.createCommunity(userId(token), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<CommunityResponse>> getMyCommunities(
            @RequestHeader("Authorization") String token) {
        return ResponseEntity.ok(communityService.getMyCommunities(userId(token)));
    }

    @GetMapping("/{communityId}")
    public ResponseEntity<CommunityResponse> getCommunity(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId) {
        return ResponseEntity.ok(communityService.getCommunityById(communityId, userId(token)));
    }

    @DeleteMapping("/{communityId}")
    public ResponseEntity<Void> deleteCommunity(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId) {
        communityService.deleteCommunity(communityId, userId(token));
        return ResponseEntity.noContent().build();
    }

    // ── Members ───────────────────────────────────────────────────────────────

    @GetMapping("/{communityId}/members")
    public ResponseEntity<CommunityMembersResponse> getCommunityMembers(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId) {
        return ResponseEntity.ok(communityService.getCommunityMembers(communityId, userId(token)));
    }

    @PostMapping("/{communityId}/members")
    public ResponseEntity<Void> addMember(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId,
            @RequestBody AddCommunityMemberRequest request) {
        communityService.addMember(communityId, userId(token), request);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{communityId}/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId,
            @PathVariable UUID targetUserId) {
        communityService.removeMember(communityId, userId(token), targetUserId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{communityId}/leave")
    public ResponseEntity<Void> leaveCommunity(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId) {
        communityService.leaveCommunity(communityId, userId(token));
        return ResponseEntity.ok().build();
    }

    // ── Join Requests ──────────────────────────────────────────────────────────

    /** Any user can request to join a community (e.g. before they have an invite link) */
    @PostMapping("/{communityId}/join-request")
    public ResponseEntity<Void> requestToJoin(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId) {
        communityService.requestToJoin(communityId, userId(token));
        return ResponseEntity.ok().build();
    }

    /** Admin fetches all pending join requests */
    @GetMapping("/{communityId}/join-requests")
    public ResponseEntity<List<JoinRequestResponse>> getPendingJoinRequests(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId) {
        return ResponseEntity.ok(
                communityService.getPendingJoinRequests(communityId, userId(token)));
    }

    /** Admin accepts or rejects a specific join request */
    @PostMapping("/{communityId}/join-requests/{requestId}/review")
    public ResponseEntity<Void> reviewJoinRequest(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId,
            @PathVariable UUID requestId,
            @RequestBody ReviewJoinRequestRequest request) {
        communityService.reviewJoinRequest(communityId, requestId, userId(token), request.isAccept());
        return ResponseEntity.ok().build();
    }

    // ── Groups ────────────────────────────────────────────────────────────────

    @PostMapping("/{communityId}/groups")
    public ResponseEntity<Void> linkGroup(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId,
            @RequestBody LinkGroupRequest request) {
        communityService.linkGroup(communityId, userId(token), request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{communityId}/groups/create")
    public ResponseEntity<CommunityGroupSummary> createGroupInCommunity(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId,
            @RequestBody CreateGroupInCommunityRequest request) {
        return ResponseEntity.ok(communityService.createGroupInCommunity(communityId, userId(token), request));
    }

    @DeleteMapping("/{communityId}/groups/{groupId}")
    public ResponseEntity<Void> unlinkGroup(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId,
            @PathVariable UUID groupId) {
        communityService.unlinkGroup(communityId, userId(token), groupId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{communityId}/groups")
    public ResponseEntity<List<CommunityGroupSummary>> getCommunityGroups(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId) {
        return ResponseEntity.ok(communityService.getCommunityGroups(communityId, userId(token)));
    }

    @GetMapping("/{communityId}/groups/{groupId}/members")
    public ResponseEntity<List<CommunityMemberPhoneView>> getGroupMembers(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId,
            @PathVariable UUID groupId) {
        return ResponseEntity.ok(communityService.getGroupMembers(communityId, groupId, userId(token)));
    }

    // ── Invite ────────────────────────────────────────────────────────────────

    @PostMapping("/{communityId}/invite")
    public ResponseEntity<GenerateInviteResponse> generateInvite(
            @RequestHeader("Authorization") String token,
            @PathVariable UUID communityId,
            @RequestBody GenerateInviteRequest request) {
        return ResponseEntity.ok(communityService.generateInviteLink(communityId, userId(token), request));
    }

    /** Public — no auth needed */
    @GetMapping("/invite/{token}")
    public ResponseEntity<InvitePreviewResponse> previewInvite(
            @PathVariable String token) {
        return ResponseEntity.ok(communityService.getInvitePreview(token));
    }

    @PostMapping("/invite/{token}/join")
    public ResponseEntity<Void> joinViaInvite(
            @RequestHeader("Authorization") String authToken,
            @PathVariable String token) {
        communityService.joinViaInvite(token, userId(authToken));
        return ResponseEntity.ok().build();
    }
}