package com.chatservice.service;

import com.chatservice.dto.ChatDtos.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface ChatService {

        // ── Private messaging ─────────────────────────────────────────────────
        MessageResponse sendPrivateMessage(UUID senderId, String senderUsername,
                        SendPrivateMessageRequest request);

        // ── Group messaging ───────────────────────────────────────────────────
        MessageResponse sendGroupMessage(UUID senderId, String senderUsername,
                        SendGroupMessageRequest request);

        // ── Fetch chats ───────────────────────────────────────────────────────
        List<ChatSummaryResponse> getChatsForUser(UUID userId);

        List<MessageResponse> getChatMessages(UUID chatId, UUID requestingUserId);

        // ADD THIS LINE ↓
        List<ChatSummaryResponse> getChatsForUserByUsername(String username);

        // ── Groups ────────────────────────────────────────────────────────────
        GroupInfo createGroup(UUID creatorId, String creatorUsername,
                        CreateGroupRequest request);

        List<GroupInfo> getGroupsForUser(UUID userId);

        void addMember(UUID groupId, UUID adminId, UUID newMemberId, String newMemberUsername);

        void removeMember(UUID groupId, UUID adminId, UUID targetUserId);

        void promoteAdmin(UUID groupId, UUID adminId, UUID targetUserId);

        List<GroupMemberResponse> getGroupMembers(UUID groupId);

        // ── Update group info (req #21) ───────────────────────────────────────
        GroupInfo updateGroupInfo(UUID groupId, UUID adminId, UpdateGroupRequest request);

        // ── Group events (req #22) ────────────────────────────────────────────
        List<GroupEventResponse> getGroupEvents(UUID groupId);

        // ── Message actions ───────────────────────────────────────────────────
        MessageResponse editMessage(UUID messageId, UUID editorId, EditMessageRequest request);

        void deleteForMe(UUID messageId, UUID userId);

        void deleteForEveryone(UUID messageId, UUID requesterId);

        // ── Media retrieval ───────────────────────────────────────────────────
        List<MediaAttachmentResponse> getChatImages(UUID chatId);

        List<MediaAttachmentResponse> getChatFiles(UUID chatId);

        List<MediaAttachmentResponse> getChatLinks(UUID chatId);

        // ── Search (req #29) ──────────────────────────────────────────────────
        List<MessageResponse> searchChat(UUID chatId, String query, UUID requestingUserId);

        List<MessageResponse> searchChatWithFilters(UUID chatId, UUID requestingUserId,
                        String query, UUID senderId,
                        String mediaType,
                        Instant from, Instant to);

        // ── Archive ───────────────────────────────────────────────────────────
        void archiveChat(UUID chatId, UUID userId);

        void unarchiveChat(UUID chatId, UUID userId);

        List<ChatSummaryResponse> getArchivedChats(UUID userId);

        // ── Search in archived chats (req #25) ───────────────────────────────
        List<MessageResponse> searchArchivedChats(UUID userId, String query);

        // ── Status ────────────────────────────────────────────────────────────
        void markDelivered(UUID chatId, UUID userId);

        void markRead(UUID chatId, UUID userId);

        // ── Wallpaper ─────────────────────────────────────────────────────────
        WallpaperResponse setWallpaper(UUID chatId, UUID userId, WallpaperRequest request);

        WallpaperResponse getWallpaper(UUID chatId, UUID userId);

        // ── Contact sharing ───────────────────────────────────────────────────
        MessageResponse sendContact(UUID senderId, String senderUsername,
                        UUID chatId, ContactPayload contact);
}