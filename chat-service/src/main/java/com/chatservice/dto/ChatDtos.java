package com.chatservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatDtos {

    // ── API Response wrapper ──────────────────────────────────────────────
    public static class ApiResponse {
        private boolean success;
        private String message;
        private Object data;

        public ApiResponse() {}
        public ApiResponse(boolean success, String message, Object data) {
            this.success = success;
            this.message = message;
            this.data = data;
        }
        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Object getData() { return data; }
    }

    // ── Error Response ────────────────────────────────────────────────────
    public static class ErrorResponse {
        private int status;
        private String error;
        private String message;
        private String path;
        private String timestamp;

        public int getStatus() { return status; }
        public void setStatus(int status) { this.status = status; }
        public String getError() { return error; }
        public void setError(String error) { this.error = error; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getPath() { return path; }
        public void setPath(String path) { this.path = path; }
        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
    }

    // ── Send Private Message Request ──────────────────────────────────────
    public static class SendPrivateMessageRequest {
        @NotBlank(message = "receiverUsername is required")
        private String receiverUsername;
        private String content;
        private String messageType = "TEXT";
        private String replyToId;
        private String fileData;
        private String fileName;
        private String fileType;
        private Long fileSizeBytes;
        private String url;
        private String previewTitle;
        private String previewDesc;
        // Contact sharing wired in (req #1)
        private ContactPayload contact;
        // Sticker support (req #2)
        private String stickerId;
        private String stickerUrl;

        public String getReceiverUsername() { return receiverUsername; }
        public void setReceiverUsername(String v) { this.receiverUsername = v; }
        public String getContent() { return content; }
        public void setContent(String v) { this.content = v; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String v) { this.messageType = v; }
        public String getReplyToId() { return replyToId; }
        public void setReplyToId(String v) { this.replyToId = v; }
        public String getFileData() { return fileData; }
        public void setFileData(String v) { this.fileData = v; }
        public String getFileName() { return fileName; }
        public void setFileName(String v) { this.fileName = v; }
        public String getFileType() { return fileType; }
        public void setFileType(String v) { this.fileType = v; }
        public Long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(Long v) { this.fileSizeBytes = v; }
        public String getUrl() { return url; }
        public void setUrl(String v) { this.url = v; }
        public String getPreviewTitle() { return previewTitle; }
        public void setPreviewTitle(String v) { this.previewTitle = v; }
        public String getPreviewDesc() { return previewDesc; }
        public void setPreviewDesc(String v) { this.previewDesc = v; }
        public ContactPayload getContact() { return contact; }
        public void setContact(ContactPayload v) { this.contact = v; }
        public String getStickerId() { return stickerId; }
        public void setStickerId(String v) { this.stickerId = v; }
        public String getStickerUrl() { return stickerUrl; }
        public void setStickerUrl(String v) { this.stickerUrl = v; }
    }

    // ── Send Group Message Request ─────────────────────────────────────────
    public static class SendGroupMessageRequest {
        @NotNull(message = "chatId is required")
        private String chatId;
        private String content;
        private String messageType = "TEXT";
        private String replyToId;
        private String fileData;
        private String fileName;
        private String fileType;
        private Long fileSizeBytes;
        private String url;
        private String previewTitle;
        private String previewDesc;
        // Contact sharing wired in (req #1)
        private ContactPayload contact;
        // Sticker support (req #2)
        private String stickerId;
        private String stickerUrl;

        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getContent() { return content; }
        public void setContent(String v) { this.content = v; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String v) { this.messageType = v; }
        public String getReplyToId() { return replyToId; }
        public void setReplyToId(String v) { this.replyToId = v; }
        public String getFileData() { return fileData; }
        public void setFileData(String v) { this.fileData = v; }
        public String getFileName() { return fileName; }
        public void setFileName(String v) { this.fileName = v; }
        public String getFileType() { return fileType; }
        public void setFileType(String v) { this.fileType = v; }
        public Long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(Long v) { this.fileSizeBytes = v; }
        public String getUrl() { return url; }
        public void setUrl(String v) { this.url = v; }
        public String getPreviewTitle() { return previewTitle; }
        public void setPreviewTitle(String v) { this.previewTitle = v; }
        public String getPreviewDesc() { return previewDesc; }
        public void setPreviewDesc(String v) { this.previewDesc = v; }
        public ContactPayload getContact() { return contact; }
        public void setContact(ContactPayload v) { this.contact = v; }
        public String getStickerId() { return stickerId; }
        public void setStickerId(String v) { this.stickerId = v; }
        public String getStickerUrl() { return stickerUrl; }
        public void setStickerUrl(String v) { this.stickerUrl = v; }
    }

    // ── Create Group Request ──────────────────────────────────────────────
    public static class CreateGroupRequest {
        @NotBlank(message = "Group name is required")
        private String name;
        private String description;
        private List<String> memberIds;
        // Added: profile picture (req #21)
        private String profilePicture;

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { this.description = v; }
        public List<String> getMemberIds() { return memberIds; }
        public void setMemberIds(List<String> v) { this.memberIds = v; }
        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String v) { this.profilePicture = v; }
    }

    // ── Update Group Request (req #21) ────────────────────────────────────
    public static class UpdateGroupRequest {
        private String name;
        private String description;
        private String profilePicture;

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { this.description = v; }
        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String v) { this.profilePicture = v; }
    }
// ── Update Group Photo Request ────────────────────────────────────────────
public static class UpdateGroupPhotoRequest {
    private String profilePicture;

    public String getProfilePicture() { return profilePicture; }
    public void setProfilePicture(String v) { this.profilePicture = v; }
}
    // ── Edit Message Request ──────────────────────────────────────────────
    public static class EditMessageRequest {
        @NotBlank(message = "content is required")
        private String content;

        public String getContent() { return content; }
        public void setContent(String v) { this.content = v; }
    }

    // ── Message Response ──────────────────────────────────────────────────
    public static class MessageResponse {
        private String id;
        private String chatId;
        private String senderId;
        private String senderUsername;
        private String messageType;
        private String content;
        private String status;
        private boolean isEdited;
        private boolean isDeleted;
        private String replyToId;
        private String sentAt;
        private String deliveredAt;
        private String readAt;
        private String editedAt;
        // Added: deletedAt and updatedAt (req DTO fix)
        private String deletedAt;
        private String updatedAt;
        private String date;
        private MediaAttachmentResponse attachment;

        public String getId() { return id; }
        public void setId(String v) { this.id = v; }
        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String v) { this.senderId = v; }
        public String getSenderUsername() { return senderUsername; }
        public void setSenderUsername(String v) { this.senderUsername = v; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String v) { this.messageType = v; }
        public String getContent() { return content; }
        public void setContent(String v) { this.content = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
        public boolean isEdited() { return isEdited; }
        public void setEdited(boolean v) { isEdited = v; }
        public boolean isDeleted() { return isDeleted; }
        public void setDeleted(boolean v) { isDeleted = v; }
        public String getReplyToId() { return replyToId; }
        public void setReplyToId(String v) { this.replyToId = v; }
        public String getSentAt() { return sentAt; }
        public void setSentAt(String v) { this.sentAt = v; }
        public String getDeliveredAt() { return deliveredAt; }
        public void setDeliveredAt(String v) { this.deliveredAt = v; }
        public String getReadAt() { return readAt; }
        public void setReadAt(String v) { this.readAt = v; }
        public String getEditedAt() { return editedAt; }
        public void setEditedAt(String v) { this.editedAt = v; }
        public String getDeletedAt() { return deletedAt; }
        public void setDeletedAt(String v) { this.deletedAt = v; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String v) { this.updatedAt = v; }
        public String getDate() { return date; }
        public void setDate(String v) { this.date = v; }
        public MediaAttachmentResponse getAttachment() { return attachment; }
        public void setAttachment(MediaAttachmentResponse v) { this.attachment = v; }
    }

    // ── Date-Grouped Messages Response (req #13) ──────────────────────────
    public static class DateGroupedMessagesResponse {
        private String date;            // e.g. "13 May 2026"
        private List<MessageResponse> messages;

        public DateGroupedMessagesResponse() {}
        public DateGroupedMessagesResponse(String date, List<MessageResponse> messages) {
            this.date = date;
            this.messages = messages;
        }
        public String getDate() { return date; }
        public void setDate(String v) { this.date = v; }
        public List<MessageResponse> getMessages() { return messages; }
        public void setMessages(List<MessageResponse> v) { this.messages = v; }
    }

    // ── Media Attachment Response ─────────────────────────────────────────
    public static class MediaAttachmentResponse {
        private String id;
        private String fileName;
        private String fileType;
        private Long fileSizeBytes;
        private String mediaType;
        private String storagePath;
        private String url;
        private String previewTitle;
        private String previewDesc;
        private String uploadedAt;

        public String getId() { return id; }
        public void setId(String v) { this.id = v; }
        public String getFileName() { return fileName; }
        public void setFileName(String v) { this.fileName = v; }
        public String getFileType() { return fileType; }
        public void setFileType(String v) { this.fileType = v; }
        public Long getFileSizeBytes() { return fileSizeBytes; }
        public void setFileSizeBytes(Long v) { this.fileSizeBytes = v; }
        public String getMediaType() { return mediaType; }
        public void setMediaType(String v) { this.mediaType = v; }
        public String getStoragePath() { return storagePath; }
        public void setStoragePath(String v) { this.storagePath = v; }
        public String getUrl() { return url; }
        public void setUrl(String v) { this.url = v; }
        public String getPreviewTitle() { return previewTitle; }
        public void setPreviewTitle(String v) { this.previewTitle = v; }
        public String getPreviewDesc() { return previewDesc; }
        public void setPreviewDesc(String v) { this.previewDesc = v; }
        public String getUploadedAt() { return uploadedAt; }
        public void setUploadedAt(String v) { this.uploadedAt = v; }
    }

    // ── Media Search Response (req #20) ───────────────────────────────────
    public static class MediaSearchResponse {
        private String messageId;
        private String chatId;
        private String senderId;
        private String senderUsername;
        private String sentAt;
        private String date;
        private MediaAttachmentResponse attachment;

        public String getMessageId() { return messageId; }
        public void setMessageId(String v) { this.messageId = v; }
        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String v) { this.senderId = v; }
        public String getSenderUsername() { return senderUsername; }
        public void setSenderUsername(String v) { this.senderUsername = v; }
        public String getSentAt() { return sentAt; }
        public void setSentAt(String v) { this.sentAt = v; }
        public String getDate() { return date; }
        public void setDate(String v) { this.date = v; }
        public MediaAttachmentResponse getAttachment() { return attachment; }
        public void setAttachment(MediaAttachmentResponse v) { this.attachment = v; }
    }

    // ── Search Result Response (req #20, #29) ────────────────────────────
    public static class SearchResultResponse {
        private String messageId;
        private String chatId;
        private String senderId;
        private String senderUsername;
        private String content;
        private String messageType;
        private String mediaType;
        private String sentAt;
        private String date;

        public String getMessageId() { return messageId; }
        public void setMessageId(String v) { this.messageId = v; }
        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String v) { this.senderId = v; }
        public String getSenderUsername() { return senderUsername; }
        public void setSenderUsername(String v) { this.senderUsername = v; }
        public String getContent() { return content; }
        public void setContent(String v) { this.content = v; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String v) { this.messageType = v; }
        public String getMediaType() { return mediaType; }
        public void setMediaType(String v) { this.mediaType = v; }
        public String getSentAt() { return sentAt; }
        public void setSentAt(String v) { this.sentAt = v; }
        public String getDate() { return date; }
        public void setDate(String v) { this.date = v; }
    }

    // ── Chat Summary Response ─────────────────────────────────────────────
    public static class ChatSummaryResponse {
        private String chatId;
        private String type;
        private String lastMessage;
        private String lastMessageAt;
        private boolean archived;
        // Added: unreadCount and otherParticipantUsername (req DTO fix)
        private int unreadCount;
        private String otherParticipantUsername;
        private GroupInfo groupInfo;

        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getType() { return type; }
        public void setType(String v) { this.type = v; }
        public String getLastMessage() { return lastMessage; }
        public void setLastMessage(String v) { this.lastMessage = v; }
        public String getLastMessageAt() { return lastMessageAt; }
        public void setLastMessageAt(String v) { this.lastMessageAt = v; }
        public boolean isArchived() { return archived; }
        public void setArchived(boolean v) { this.archived = v; }
        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int v) { this.unreadCount = v; }
        public String getOtherParticipantUsername() { return otherParticipantUsername; }
        public void setOtherParticipantUsername(String v) { this.otherParticipantUsername = v; }
        public GroupInfo getGroupInfo() { return groupInfo; }
        public void setGroupInfo(GroupInfo v) { this.groupInfo = v; }
    }

    // ── Group Info ────────────────────────────────────────────────────────
    public static class GroupInfo {
        private String groupId;
        private String name;
        private String description;
        // Added: profilePicture (req DTO fix)
        private String profilePicture;
        private int memberCount;
        private String creatorId;
        private String createdAt;

        public String getGroupId() { return groupId; }
        public void setGroupId(String v) { this.groupId = v; }
        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { this.description = v; }
        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String v) { this.profilePicture = v; }
        public int getMemberCount() { return memberCount; }
        public void setMemberCount(int v) { this.memberCount = v; }
        public String getCreatorId() { return creatorId; }
        public void setCreatorId(String v) { this.creatorId = v; }
        public String getCreatedAt() { return createdAt; }
        public void setCreatedAt(String v) { this.createdAt = v; }
    }

    // ── Group Member Response ─────────────────────────────────────────────
    public static class GroupMemberResponse {
        private String userId;
        private String username;
        private String role;
        private String joinedAt;
        // Added: leftAt (req DTO fix)
        private String leftAt;

        public String getUserId() { return userId; }
        public void setUserId(String v) { this.userId = v; }
        public String getUsername() { return username; }
        public void setUsername(String v) { this.username = v; }
        public String getRole() { return role; }
        public void setRole(String v) { this.role = v; }
        public String getJoinedAt() { return joinedAt; }
        public void setJoinedAt(String v) { this.joinedAt = v; }
        public String getLeftAt() { return leftAt; }
        public void setLeftAt(String v) { this.leftAt = v; }
    }

    // ── Group Event Response (req #22) ────────────────────────────────────
    public static class GroupEventResponse {
        private String eventId;
        private String groupId;
        private String eventType;
        private String actorId;
        private String targetId;
        private String description;
        private String occurredAt;

        public String getEventId() { return eventId; }
        public void setEventId(String v) { this.eventId = v; }
        public String getGroupId() { return groupId; }
        public void setGroupId(String v) { this.groupId = v; }
        public String getEventType() { return eventType; }
        public void setEventType(String v) { this.eventType = v; }
        public String getActorId() { return actorId; }
        public void setActorId(String v) { this.actorId = v; }
        public String getTargetId() { return targetId; }
        public void setTargetId(String v) { this.targetId = v; }
        public String getDescription() { return description; }
        public void setDescription(String v) { this.description = v; }
        public String getOccurredAt() { return occurredAt; }
        public void setOccurredAt(String v) { this.occurredAt = v; }
    }

    // ── Archive Chat Request (req #23) ────────────────────────────────────
    public static class ArchiveChatRequest {
        @NotNull(message = "chatId is required")
        private String chatId;

        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
    }

    // ── Kafka Chat Archived Event (req #24) ───────────────────────────────
    public static class KafkaChatArchivedEvent {
        private String chatId;
        private String userId;
        private String archivedAt;
        private String eventType = "CHAT_ARCHIVED";

        public KafkaChatArchivedEvent() {}
        public KafkaChatArchivedEvent(String chatId, String userId, String archivedAt) {
            this.chatId = chatId;
            this.userId = userId;
            this.archivedAt = archivedAt;
        }
        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getUserId() { return userId; }
        public void setUserId(String v) { this.userId = v; }
        public String getArchivedAt() { return archivedAt; }
        public void setArchivedAt(String v) { this.archivedAt = v; }
        public String getEventType() { return eventType; }
        public void setEventType(String v) { this.eventType = v; }
    }

    // ── WebSocket Message ─────────────────────────────────────────────────
    public static class WsMessage {
        private String type;
        private String chatId;
        private String senderId;
        private String senderUsername;
        private String content;
        private String messageType;
        private String messageId;
        private String status;
        private String sentAt;
        private boolean isEdited;
        // Added: isDeleted, replyToId, date (req DTO fix)
        private boolean isDeleted;
        private String replyToId;
        private String date;
        private MediaAttachmentResponse attachment;

        public String getType() { return type; }
        public void setType(String v) { this.type = v; }
        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getSenderId() { return senderId; }
        public void setSenderId(String v) { this.senderId = v; }
        public String getSenderUsername() { return senderUsername; }
        public void setSenderUsername(String v) { this.senderUsername = v; }
        public String getContent() { return content; }
        public void setContent(String v) { this.content = v; }
        public String getMessageType() { return messageType; }
        public void setMessageType(String v) { this.messageType = v; }
        public String getMessageId() { return messageId; }
        public void setMessageId(String v) { this.messageId = v; }
        public String getStatus() { return status; }
        public void setStatus(String v) { this.status = v; }
        public String getSentAt() { return sentAt; }
        public void setSentAt(String v) { this.sentAt = v; }
        public boolean isEdited() { return isEdited; }
        public void setEdited(boolean v) { isEdited = v; }
        public boolean isDeleted() { return isDeleted; }
        public void setDeleted(boolean v) { isDeleted = v; }
        public String getReplyToId() { return replyToId; }
        public void setReplyToId(String v) { this.replyToId = v; }
        public String getDate() { return date; }
        public void setDate(String v) { this.date = v; }
        public MediaAttachmentResponse getAttachment() { return attachment; }
        public void setAttachment(MediaAttachmentResponse v) { this.attachment = v; }
    }

    // ── Contact Payload ───────────────────────────────────────────────────
    public static class ContactPayload {
        private String name;
        private String phoneNumber;
        private String email;
        private String profilePicture;

        public String getName() { return name; }
        public void setName(String v) { this.name = v; }
        public String getPhoneNumber() { return phoneNumber; }
        public void setPhoneNumber(String v) { this.phoneNumber = v; }
        public String getEmail() { return email; }
        public void setEmail(String v) { this.email = v; }
        public String getProfilePicture() { return profilePicture; }
        public void setProfilePicture(String v) { this.profilePicture = v; }
    }

    // ── Wallpaper Request ─────────────────────────────────────────────────
    public static class WallpaperRequest {
        // Added @NotBlank validation (req DTO fix)
        @NotBlank(message = "wallpaperType is required")
        private String wallpaperType;
        private String wallpaperData;
        private String wallpaperColor;

        public String getWallpaperType() { return wallpaperType; }
        public void setWallpaperType(String v) { this.wallpaperType = v; }
        public String getWallpaperData() { return wallpaperData; }
        public void setWallpaperData(String v) { this.wallpaperData = v; }
        public String getWallpaperColor() { return wallpaperColor; }
        public void setWallpaperColor(String v) { this.wallpaperColor = v; }
    }

    // ── Wallpaper Response ────────────────────────────────────────────────
    public static class WallpaperResponse {
        private String chatId;
        private String userId;
        private String wallpaperType;
        private String wallpaperData;
        private String wallpaperColor;
        private String updatedAt;

        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getUserId() { return userId; }
        public void setUserId(String v) { this.userId = v; }
        public String getWallpaperType() { return wallpaperType; }
        public void setWallpaperType(String v) { this.wallpaperType = v; }
        public String getWallpaperData() { return wallpaperData; }
        public void setWallpaperData(String v) { this.wallpaperData = v; }
        public String getWallpaperColor() { return wallpaperColor; }
        public void setWallpaperColor(String v) { this.wallpaperColor = v; }
        public String getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(String v) { this.updatedAt = v; }
    }

    // ── WebRTC Signal ─────────────────────────────────────────────────────
    public static class WebRtcSignal {
        private String type;
        private String sessionId;
        private String chatId;
        private String callerId;
        private String calleeId;
        private String fromUserId;
        private String callType;
        private String sdp;
        private String candidate;
        private String sdpMid;
        private Integer sdpMLineIndex;
        private Long durationSeconds;

        public String getType() { return type; }
        public void setType(String v) { this.type = v; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String v) { this.sessionId = v; }
        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getCallerId() { return callerId; }
        public void setCallerId(String v) { this.callerId = v; }
        public String getCalleeId() { return calleeId; }
        public void setCalleeId(String v) { this.calleeId = v; }
        public String getFromUserId() { return fromUserId; }
        public void setFromUserId(String v) { this.fromUserId = v; }
        public String getCallType() { return callType; }
        public void setCallType(String v) { this.callType = v; }
        public String getSdp() { return sdp; }
        public void setSdp(String v) { this.sdp = v; }
        public String getCandidate() { return candidate; }
        public void setCandidate(String v) { this.candidate = v; }
        public String getSdpMid() { return sdpMid; }
        public void setSdpMid(String v) { this.sdpMid = v; }
        public Integer getSdpMLineIndex() { return sdpMLineIndex; }
        public void setSdpMLineIndex(Integer v) { this.sdpMLineIndex = v; }
        public Long getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(Long v) { this.durationSeconds = v; }
    }

    // ── Call Session Response ─────────────────────────────────────────────
    public static class CallSessionResponse {
        private String sessionId;
        private String chatId;
        private String callerId;
        private String calleeId;
        private String callType;
        private String callStatus;
        private String startedAt;
        private String answeredAt;
        private String endedAt;
        private Long durationSeconds;

        public String getSessionId() { return sessionId; }
        public void setSessionId(String v) { this.sessionId = v; }
        public String getChatId() { return chatId; }
        public void setChatId(String v) { this.chatId = v; }
        public String getCallerId() { return callerId; }
        public void setCallerId(String v) { this.callerId = v; }
        public String getCalleeId() { return calleeId; }
        public void setCalleeId(String v) { this.calleeId = v; }
        public String getCallType() { return callType; }
        public void setCallType(String v) { this.callType = v; }
        public String getCallStatus() { return callStatus; }
        public void setCallStatus(String v) { this.callStatus = v; }
        public String getStartedAt() { return startedAt; }
        public void setStartedAt(String v) { this.startedAt = v; }
        public String getAnsweredAt() { return answeredAt; }
        public void setAnsweredAt(String v) { this.answeredAt = v; }
        public String getEndedAt() { return endedAt; }
        public void setEndedAt(String v) { this.endedAt = v; }
        public Long getDurationSeconds() { return durationSeconds; }
        public void setDurationSeconds(Long v) { this.durationSeconds = v; }
    }
}