package com.chatservice.controller;

import com.chatservice.dto.ChatDtos.*;
import com.chatservice.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    // ── GET /chats/{userId} — get all chats for a user ──────────────────────
    @GetMapping("/chats/{userId}")
    public ResponseEntity<ApiResponse> getChats(
            @PathVariable UUID userId,
            Authentication auth) {
        log.info("GET /chats/{}", userId);
        List<ChatSummaryResponse> chats = chatService.getChatsForUser(userId);
        return ok("Chats fetched successfully.", chats);
    }

    // ── GET /chats/{chatId}/messages ─────────────────────────────────────────
    @GetMapping("/chats/{chatId}/messages")
    public ResponseEntity<ApiResponse> getChatMessages(
            @PathVariable UUID chatId,
            Authentication auth) {
        UUID userId = uuid(auth);
        log.info("GET /chats/{}/messages userId={}", chatId, userId);
        List<MessageResponse> messages = chatService.getChatMessages(chatId, userId);
        return ok("Messages fetched successfully.", messages);
    }

    // ── POST /chats/private — send private message ───────────────────────────
    @PostMapping("/chats/private")
    public ResponseEntity<ApiResponse> sendPrivateMessage(
            @Valid @RequestBody SendPrivateMessageRequest request,
            Authentication auth,
            @RequestAttribute(required = false) String username) {
        UUID senderId = uuid(auth);
        log.info("POST /chats/private sender={}", senderId);
        MessageResponse msg = chatService.sendPrivateMessage(senderId, username, request);
        return created("Message sent.", msg);
    }

    // ── POST /chats/group — send group message ───────────────────────────────
    @PostMapping("/chats/group")
    public ResponseEntity<ApiResponse> sendGroupMessage(
            @Valid @RequestBody SendGroupMessageRequest request,
            Authentication auth,
            @RequestAttribute(required = false) String username) {
        UUID senderId = uuid(auth);
        log.info("POST /chats/group sender={} chatId={}", senderId, request.getChatId());
        MessageResponse msg = chatService.sendGroupMessage(senderId, username, request);
        return created("Message sent.", msg);
    }

    // ── POST /chats/{chatId}/contact — send contact (req #2) ─────────────────
    @PostMapping("/chats/{chatId}/contact")
    public ResponseEntity<ApiResponse> sendContact(
            @PathVariable UUID chatId,
            @RequestBody ContactPayload contact,
            Authentication auth,
            @RequestAttribute(required = false) String username) {
        UUID senderId = uuid(auth);
        log.info("POST /chats/{}/contact sender={}", chatId, senderId);
        MessageResponse msg = chatService.sendContact(senderId, username, chatId, contact);
        return created("Contact sent.", msg);
    }

    // ── GET /groups/{userId} — list all groups for user ──────────────────────
    @GetMapping("/groups/{userId}")
    public ResponseEntity<ApiResponse> getGroups(@PathVariable UUID userId) {
        log.info("GET /groups/{}", userId);
        List<GroupInfo> groups = chatService.getGroupsForUser(userId);
        return ok("Groups fetched successfully.", groups);
    }

    // ── POST /groups — create group ──────────────────────────────────────────
    @PostMapping("/groups")
    public ResponseEntity<ApiResponse> createGroup(
            @Valid @RequestBody CreateGroupRequest request,
            Authentication auth,
            @RequestAttribute(required = false) String username) {
        UUID creatorId = uuid(auth);
        log.info("POST /groups creator={} name={}", creatorId, request.getName());
        GroupInfo group = chatService.createGroup(creatorId, username, request);
        return created("Group created successfully.", group);
    }

    // ── PUT /groups/{groupId} — update group info (req #21) ──────────────────
    @PutMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse> updateGroup(
            @PathVariable UUID groupId,
            @RequestBody UpdateGroupRequest request,
            Authentication auth) {
        UUID adminId = uuid(auth);
        log.info("PUT /groups/{} admin={}", groupId, adminId);
        GroupInfo group = chatService.updateGroupInfo(groupId, adminId, request);
        return ok("Group updated.", group);
    }

    // ── GET /groups/{groupId}/members ────────────────────────────────────────
    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<ApiResponse> getMembers(@PathVariable UUID groupId) {
        List<GroupMemberResponse> members = chatService.getGroupMembers(groupId);
        return ok("Members fetched.", members);
    }

    // ── POST /groups/{groupId}/members ───────────────────────────────────────
    @PostMapping("/groups/{groupId}/members")
    public ResponseEntity<ApiResponse> addMember(
            @PathVariable UUID groupId,
            @RequestParam UUID memberId,
            @RequestParam(required = false) String memberUsername,
            Authentication auth) {
        UUID adminId = uuid(auth);
        chatService.addMember(groupId, adminId, memberId, memberUsername);
        return ok("Member added.", null);
    }

    // ── DELETE /groups/{groupId}/members/{memberId} ──────────────────────────
    @DeleteMapping("/groups/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            Authentication auth) {
        UUID adminId = uuid(auth);
        chatService.removeMember(groupId, adminId, memberId);
        return ok("Member removed.", null);
    }

    // ── POST /groups/{groupId}/admins/{userId} ───────────────────────────────
    @PostMapping("/groups/{groupId}/admins/{userId}")
    public ResponseEntity<ApiResponse> promoteAdmin(
            @PathVariable UUID groupId,
            @PathVariable UUID userId,
            Authentication auth) {
        UUID adminId = uuid(auth);
        chatService.promoteAdmin(groupId, adminId, userId);
        return ok("User promoted to admin.", null);
    }

    // ── GET /groups/{groupId}/events — group event history (req #22) ─────────
    @GetMapping("/groups/{groupId}/events")
    public ResponseEntity<ApiResponse> getGroupEvents(@PathVariable UUID groupId) {
        log.info("GET /groups/{}/events", groupId);
        List<GroupEventResponse> events = chatService.getGroupEvents(groupId);
        return ok("Group events fetched.", events);
    }

    // ── PUT /messages/{messageId} — edit message ─────────────────────────────
    @PutMapping("/messages/{messageId}")
    public ResponseEntity<ApiResponse> editMessage(
            @PathVariable UUID messageId,
            @Valid @RequestBody EditMessageRequest request,
            Authentication auth) {
        UUID userId = uuid(auth);
        log.info("PUT /messages/{} editor={}", messageId, userId);
        MessageResponse msg = chatService.editMessage(messageId, userId, request);
        return ok("Message edited.", msg);
    }

    // ── DELETE /messages/{messageId}/me — delete for me ──────────────────────
    @DeleteMapping("/messages/{messageId}/me")
    public ResponseEntity<ApiResponse> deleteForMe(
            @PathVariable UUID messageId,
            Authentication auth) {
        UUID userId = uuid(auth);
        chatService.deleteForMe(messageId, userId);
        return ok("Message deleted for you.", null);
    }

    // ── DELETE /messages/{messageId}/everyone — delete for everyone ───────────
    @DeleteMapping("/messages/{messageId}/everyone")
    public ResponseEntity<ApiResponse> deleteForEveryone(
            @PathVariable UUID messageId,
            Authentication auth) {
        UUID userId = uuid(auth);
        chatService.deleteForEveryone(messageId, userId);
        return ok("Message deleted for everyone.", null);
    }

    // ── GET /chats/{chatId}/images (req #19) ─────────────────────────────────
    @GetMapping("/chats/{chatId}/images")
    public ResponseEntity<ApiResponse> getChatImages(@PathVariable UUID chatId) {
        log.info("GET /chats/{}/images", chatId);
        return ok("Images fetched.", chatService.getChatImages(chatId));
    }

    // ── GET /chats/{chatId}/files (req #19) ──────────────────────────────────
    @GetMapping("/chats/{chatId}/files")
    public ResponseEntity<ApiResponse> getChatFiles(@PathVariable UUID chatId) {
        log.info("GET /chats/{}/files", chatId);
        return ok("Files fetched.", chatService.getChatFiles(chatId));
    }

    // ── GET /chats/{chatId}/links (req #19) ──────────────────────────────────
    @GetMapping("/chats/{chatId}/links")
    public ResponseEntity<ApiResponse> getChatLinks(@PathVariable UUID chatId) {
        log.info("GET /chats/{}/links", chatId);
        return ok("Links fetched.", chatService.getChatLinks(chatId));
    }

    // ── GET /chats/{chatId}/search — basic search (req #20) ──────────────────
    @GetMapping("/chats/{chatId}/search")
    public ResponseEntity<ApiResponse> searchChat(
            @PathVariable UUID chatId,
            @RequestParam String query,
            Authentication auth) {
        UUID userId = uuid(auth);
        log.info("GET /chats/{}/search query={} userId={}", chatId, query, userId);
        return ok("Search results.", chatService.searchChat(chatId, query, userId));
    }

    // ── GET /chats/{chatId}/search/advanced — search with filters (req #29) ───
    // Supports: query, senderId, mediaType, from (ISO instant), to (ISO instant)
    @GetMapping("/chats/{chatId}/search/advanced")
    public ResponseEntity<ApiResponse> searchChatAdvanced(
            @PathVariable UUID chatId,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) UUID senderId,
            @RequestParam(required = false) String mediaType,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            Authentication auth) {
        UUID userId = uuid(auth);
        log.info("GET /chats/{}/search/advanced query={} sender={} mediaType={}",
                chatId, query, senderId, mediaType);
        Instant fromInstant = from != null ? Instant.parse(from) : null;
        Instant toInstant   = to   != null ? Instant.parse(to)   : null;
        return ok("Search results.",
                chatService.searchChatWithFilters(chatId, userId, query,
                        senderId, mediaType, fromInstant, toInstant));
    }

    // ── POST /chats/{chatId}/archive (req #23) ────────────────────────────────
    @PostMapping("/chats/{chatId}/archive")
    public ResponseEntity<ApiResponse> archiveChat(
            @PathVariable UUID chatId,
            Authentication auth) {
        UUID userId = uuid(auth);
        chatService.archiveChat(chatId, userId);
        return ok("Chat archived.", null);
    }

    // ── DELETE /chats/{chatId}/archive (req #23) ──────────────────────────────
    @DeleteMapping("/chats/{chatId}/archive")
    public ResponseEntity<ApiResponse> unarchiveChat(
            @PathVariable UUID chatId,
            Authentication auth) {
        UUID userId = uuid(auth);
        chatService.unarchiveChat(chatId, userId);
        return ok("Chat unarchived.", null);
    }

    // ── GET /chats/archived (req #23) ─────────────────────────────────────────
    @GetMapping("/chats/archived")
    public ResponseEntity<ApiResponse> getArchivedChats(Authentication auth) {
        UUID userId = uuid(auth);
        return ok("Archived chats fetched.", chatService.getArchivedChats(userId));
    }

    // ── GET /chats/archived/search — search in archived chats (req #25) ───────
    @GetMapping("/chats/archived/search")
    public ResponseEntity<ApiResponse> searchArchivedChats(
            @RequestParam String query,
            Authentication auth) {
        UUID userId = uuid(auth);
        log.info("GET /chats/archived/search query={} userId={}", query, userId);
        return ok("Archived search results.", chatService.searchArchivedChats(userId, query));
    }

    // ── POST /chats/{chatId}/delivered (req #4) ───────────────────────────────
    @PostMapping("/chats/{chatId}/delivered")
    public ResponseEntity<ApiResponse> markDelivered(
            @PathVariable UUID chatId,
            Authentication auth) {
        UUID userId = uuid(auth);
        chatService.markDelivered(chatId, userId);
        return ok("Messages marked as delivered.", null);
    }

    // ── POST /chats/{chatId}/read (req #4) ────────────────────────────────────
    @PostMapping("/chats/{chatId}/read")
    public ResponseEntity<ApiResponse> markRead(
            @PathVariable UUID chatId,
            Authentication auth) {
        UUID userId = uuid(auth);
        chatService.markRead(chatId, userId);
        return ok("Messages marked as read.", null);
    }

    // ── GET /chats/{chatId}/wallpaper (req #2) ────────────────────────────────
    @GetMapping("/chats/{chatId}/wallpaper")
    public ResponseEntity<ApiResponse> getWallpaper(
            @PathVariable UUID chatId,
            Authentication auth) {
        UUID userId = uuid(auth);
        log.info("GET /chats/{}/wallpaper userId={}", chatId, userId);
        return ok("Wallpaper fetched.", chatService.getWallpaper(chatId, userId));
    }

    // ── PUT /chats/{chatId}/wallpaper (req #2) ────────────────────────────────
    @PutMapping("/chats/{chatId}/wallpaper")
    public ResponseEntity<ApiResponse> setWallpaper(
            @PathVariable UUID chatId,
            @Valid @RequestBody WallpaperRequest request,
            Authentication auth) {
        UUID userId = uuid(auth);
        log.info("PUT /chats/{}/wallpaper userId={}", chatId, userId);
        return ok("Wallpaper updated.", chatService.setWallpaper(chatId, userId, request));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private UUID uuid(Authentication auth) {
        return UUID.fromString((String) auth.getPrincipal());
    }

    private ResponseEntity<ApiResponse> ok(String message, Object data) {
        return ResponseEntity.ok(new ApiResponse(true, message, data));
    }

    private ResponseEntity<ApiResponse> created(String message, Object data) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse(true, message, data));
    }
}
