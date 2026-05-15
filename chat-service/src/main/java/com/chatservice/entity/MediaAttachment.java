package com.chatservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "media_attachments", indexes = {
    @Index(name = "idx_media_message_id", columnList = "message_id"),
    @Index(name = "idx_media_chat_id",    columnList = "chat_id"),
    @Index(name = "idx_media_type",       columnList = "media_type")
})
public class MediaAttachment {

    public enum MediaType { IMAGE, FILE, CONTACT, STICKER, LINK }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Direct column — no JOIN ON needed anywhere
    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    // Store chatId directly so we can query by chat without joining messages
    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false)
    private MediaType mediaType;

    // ── Scalability fix ───────────────────────────────────────────────────
    // Base64 data is NOT stored in DB — store file path/S3 key instead.
    // For contacts/stickers (small payloads) we store JSON string here.
    @Column(name = "storage_path", columnDefinition = "TEXT")
    private String storagePath;  // e.g. "uploads/chatId/filename.jpg" or S3 key

    // Small structured payloads only (contacts, stickers) — not binary files
    @Column(name = "payload", columnDefinition = "TEXT")
    private String payload;

    @Column(columnDefinition = "TEXT")
    private String url;          // for LINK type

    @Column(name = "preview_title")
    private String previewTitle;

    @Column(name = "preview_desc", columnDefinition = "TEXT")
    private String previewDesc;

    @Column(name = "uploaded_at", nullable = false)
    private Instant uploadedAt;

    @PrePersist
    void prePersist() { if (uploadedAt == null) uploadedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    public UUID getChatId() { return chatId; }
    public void setChatId(UUID chatId) { this.chatId = chatId; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(Long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }
    public MediaType getMediaType() { return mediaType; }
    public void setMediaType(MediaType mediaType) { this.mediaType = mediaType; }
    public String getStoragePath() { return storagePath; }
    public void setStoragePath(String storagePath) { this.storagePath = storagePath; }
    public String getPayload() { return payload; }
    public void setPayload(String payload) { this.payload = payload; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getPreviewTitle() { return previewTitle; }
    public void setPreviewTitle(String previewTitle) { this.previewTitle = previewTitle; }
    public String getPreviewDesc() { return previewDesc; }
    public void setPreviewDesc(String previewDesc) { this.previewDesc = previewDesc; }
    public Instant getUploadedAt() { return uploadedAt; }
}
