package com.chatservice.controller;

import com.chatservice.dto.ChatDtos.*;
import com.chatservice.service.ChatService;
import com.chatservice.websocket.PresenceEventListener; // ✅ add this
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;   // ✅ add this
import java.util.Set;   // ✅ add this
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;
    private final PresenceEventListener presenceEventListener; // ✅ add this

    // ✅ inject via constructor
    public ChatController(ChatService chatService,
                          PresenceEventListener presenceEventListener) {
        this.chatService            = chatService;
        this.presenceEventListener  = presenceEventListener;
    }

    // ── all your existing endpoints unchanged ──────────────────────────────

    @GetMapping("/chats/{userId}")
    public ResponseEntity<ApiResponse> getChats(
            @PathVariable UUID userId, Authentication auth) {
        log.info("GET /chats/{}", userId);
        List<ChatSummaryResponse> chats = chatService.getChatsForUser(userId);
        return ok("Chats fetched successfully.", chats);
    }

    @GetMapping("/chats/username/{username}")
    public ResponseEntity<ApiResponse> getChatsByUsername(
            @PathVariable String username, Authentication auth) {
        log.info("GET /chats/username/{}", username);
        List<ChatSummaryResponse> chats = chatService.getChatsForUserByUsername(username);
        return ok("Chats fetched successfully.", chats);
    }

    @GetMapping("/chats/{chatId}/messages")
    public ResponseEntity<ApiResponse> getChatMessages(
            @PathVariable UUID chatId, Authentication auth) {
        UUID userId = uuid(auth);
        log.info("GET /chats/{}/messages userId={}", chatId, userId);
        List<MessageResponse> messages = chatService.getChatMessages(chatId, userId);
        return ok("Messages fetched successfully.", messages);
    }

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

    @GetMapping("/groups/{userId}")
    public ResponseEntity<ApiResponse> getGroups(@PathVariable UUID userId) {
        log.info("GET /groups/{}", userId);
        List<GroupInfo> groups = chatService.getGroupsForUser(userId);
        return ok("Groups fetched successfully.", groups);
    }

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

    @GetMapping("/groups/{groupId}/members")
    public ResponseEntity<ApiResponse> getMembers(@PathVariable UUID groupId) {
        List<GroupMemberResponse> members = chatService.getGroupMembers(groupId);
        return ok("Members fetched.", members);
    }

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

    @DeleteMapping("/groups/{groupId}/members/{memberId}")
    public ResponseEntity<ApiResponse> removeMember(
            @PathVariable UUID groupId,
            @PathVariable UUID memberId,
            Authentication auth) {
        UUID adminId = uuid(auth);
        chatService.removeMember(groupId, adminId, memberId);
        return ok("Member removed.", null);
    }

    @PostMapping("/groups/{groupId}/admins/{userId}")
    public ResponseEntity<ApiResponse> promoteAdmin(
            @PathVariable UUID groupId,
            @PathVariable UUID userId,
            Authentication auth) {
        UUID adminId = uuid(auth);
        chatService.promoteAdmin(groupId, adminId, userId);
        return ok("User promoted to admin.", null);
    }

    @GetMapping("/groups/{groupId}/events")
    public ResponseEntity<ApiResponse> getGroupEvents(@PathVariable UUID groupId) {
        log.info("GET /groups/{}/events", groupId);
        List<GroupEventResponse> events = chatService.getGroupEvents(groupId);
        return ok("Group events fetched.", events);
    }

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

    @DeleteMapping("/messages/{messageId}/me")
    public ResponseEntity<ApiResponse> deleteForMe(
            @PathVariable UUID messageId, Authentication auth) {
        UUID userId = uuid(auth);
        chatService.deleteForMe(messageId, userId);
        return ok("Message deleted for you.", null);
    }

    @DeleteMapping("/messages/{messageId}/everyone")
    public ResponseEntity<ApiResponse> deleteForEveryone(
            @PathVariable UUID messageId, Authentication auth) {
        UUID userId = uuid(auth);
        chatService.deleteForEveryone(messageId, userId);
        return ok("Message deleted for everyone.", null);
    }

    // ── Star ─────────────────────────────────────────────────────────────────

    @PostMapping("/messages/{messageId}/star")
    public ResponseEntity<ApiResponse> starMessage(
            @PathVariable UUID messageId, Authentication auth) {
        UUID userId = uuid(auth);
        log.info("POST /messages/{}/star user={}", messageId, userId);
        MessageResponse msg = chatService.starMessage(messageId, userId);
        return ok("Message starred.", msg);
    }

    @DeleteMapping("/messages/{messageId}/star")
    public ResponseEntity<ApiResponse> unstarMessage(
            @PathVariable UUID messageId, Authentication auth) {
        UUID userId = uuid(auth);
        log.info("DELETE /messages/{}/star user={}", messageId, userId);
        MessageResponse msg = chatService.unstarMessage(messageId, userId);
        return ok("Message unstarred.", msg);
    }

    @GetMapping("/messages/starred")
    public ResponseEntity<ApiResponse> getStarredMessages(Authentication auth) {
        UUID userId = uuid(auth);
        log.info("GET /messages/starred user={}", userId);
        List<MessageResponse> starred = chatService.getStarredMessages(userId);
        return ok("Starred messages fetched.", starred);
    }

    // ── Forward ──────────────────────────────────────────────────────────────

    @PostMapping("/messages/forward")
    public ResponseEntity<ApiResponse> forwardMessage(
            @Valid @RequestBody ForwardMessageRequest request,
            Authentication auth,
            @RequestAttribute(required = false) String username) {
        UUID senderId = uuid(auth);
        log.info("POST /messages/forward sender={} messageId={} targets={}",
                senderId, request.getMessageId(), request.getTargetChatIds());
        List<MessageResponse> forwarded = chatService.forwardMessage(senderId, username, request);
        return created("Message forwarded.", forwarded);
    }

    @GetMapping("/chats/{chatId}/images")
    public ResponseEntity<ApiResponse> getChatImages(@PathVariable UUID chatId) {
        return ok("Images fetched.", chatService.getChatImages(chatId));
    }

    @GetMapping("/chats/{chatId}/files")
    public ResponseEntity<ApiResponse> getChatFiles(@PathVariable UUID chatId) {
        return ok("Files fetched.", chatService.getChatFiles(chatId));
    }

    @GetMapping("/chats/{chatId}/links")
    public ResponseEntity<ApiResponse> getChatLinks(@PathVariable UUID chatId) {
        return ok("Links fetched.", chatService.getChatLinks(chatId));
    }

    @GetMapping("/chats/{chatId}/search")
    public ResponseEntity<ApiResponse> searchChat(
            @PathVariable UUID chatId,
            @RequestParam String query,
            Authentication auth) {
        UUID userId = uuid(auth);
        return ok("Search results.", chatService.searchChat(chatId, query, userId));
    }

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
        Instant fromInstant = from != null ? Instant.parse(from) : null;
        Instant toInstant   = to   != null ? Instant.parse(to)   : null;
        return ok("Search results.",
                chatService.searchChatWithFilters(chatId, userId, query,
                        senderId, mediaType, fromInstant, toInstant));
    }

    @PostMapping("/chats/{chatId}/archive")
    public ResponseEntity<ApiResponse> archiveChat(
            @PathVariable UUID chatId, Authentication auth) {
        UUID userId = uuid(auth);
        chatService.archiveChat(chatId, userId);
        return ok("Chat archived.", null);
    }

    @DeleteMapping("/chats/{chatId}/archive")
    public ResponseEntity<ApiResponse> unarchiveChat(
            @PathVariable UUID chatId, Authentication auth) {
        UUID userId = uuid(auth);
        chatService.unarchiveChat(chatId, userId);
        return ok("Chat unarchived.", null);
    }

    @GetMapping("/chats/archived")
    public ResponseEntity<ApiResponse> getArchivedChats(Authentication auth) {
        UUID userId = uuid(auth);
        return ok("Archived chats fetched.", chatService.getArchivedChats(userId));
    }

    @GetMapping("/chats/archived/search")
    public ResponseEntity<ApiResponse> searchArchivedChats(
            @RequestParam String query, Authentication auth) {
        UUID userId = uuid(auth);
        return ok("Archived search results.", chatService.searchArchivedChats(userId, query));
    }

    @PostMapping("/chats/{chatId}/delivered")
    public ResponseEntity<ApiResponse> markDelivered(
            @PathVariable UUID chatId, Authentication auth) {
        UUID userId = uuid(auth);
        chatService.markDelivered(chatId, userId);
        return ok("Messages marked as delivered.", null);
    }

    @PostMapping("/chats/{chatId}/read")
    public ResponseEntity<ApiResponse> markRead(
            @PathVariable UUID chatId, Authentication auth) {
        UUID userId = uuid(auth);
        chatService.markRead(chatId, userId);
        return ok("Messages marked as read.", null);
    }

    @GetMapping("/chats/{chatId}/wallpaper")
    public ResponseEntity<ApiResponse> getWallpaper(@PathVariable UUID chatId) {
        return ok("Wallpaper fetched.", chatService.getWallpaper(chatId));
    }

    @PutMapping("/chats/{chatId}/wallpaper")
    public ResponseEntity<ApiResponse> setWallpaper(
            @PathVariable UUID chatId,
            @Valid @RequestBody WallpaperRequest request) {
        return ok("Wallpaper updated.", chatService.setWallpaper(chatId, request));
    }

    @DeleteMapping("/chats/{chatId}/clear-for-me")
    public ResponseEntity<ApiResponse> clearChatForMe(
            @PathVariable UUID chatId, Authentication auth) {
        UUID userId = uuid(auth);
        chatService.clearChatForMe(chatId, userId);
        return ok("Chat cleared for you.", null);
    }

    @DeleteMapping("/groups/{groupId}/exit")
    public ResponseEntity<ApiResponse> exitGroup(
            @PathVariable UUID groupId, Authentication auth) {
        UUID userId = uuid(auth);
        chatService.exitGroup(groupId, userId);
        return ok("You have left the group.", null);
    }

    @PutMapping(value = "/groups/{groupId}/photo",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse> updateGroupPhoto(
            @PathVariable UUID groupId,
            @RequestPart("photo") org.springframework.web.multipart.MultipartFile photo,
            Authentication auth) throws IOException {
        UUID userId = uuid(auth);
        GroupInfo group = chatService.updateGroupPhoto(groupId, userId, photo.getBytes());
        return ok("Group photo updated.", group);
    }

    @GetMapping("/groups/{groupId}/photo")
    public ResponseEntity<byte[]> getGroupPhoto(@PathVariable UUID groupId) {
        byte[] photo = chatService.getGroupPhoto(groupId);
        if (photo == null) return ResponseEntity.notFound().build();
        return ResponseEntity.ok()
                .header("Content-Type", "image/jpeg")
                .header("Cache-Control", "max-age=86400")
                .body(photo);
    }

    @DeleteMapping("/groups/{groupId}")
    public ResponseEntity<ApiResponse> deleteGroup(
            @PathVariable UUID groupId, Authentication auth) {
        UUID userId = uuid(auth);
        chatService.deleteGroup(groupId, userId);
        return ok("Group deleted.", null);
    }

    // ✅ Presence endpoint — returns all currently online usernames
    @GetMapping("/presence/online")
    public ResponseEntity<?> getOnlineUsers() {
        Set<String> onlineUsers = presenceEventListener.getOnlineUsernames();
        return ResponseEntity.ok(Map.of("data", List.copyOf(onlineUsers)));
    }

    // ── helpers ───────────────────────────────────────────────────────────────

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