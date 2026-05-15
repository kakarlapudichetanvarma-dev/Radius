package com.chatservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "chat_search_index", indexes = {
    @Index(name = "idx_csi_chat_id",   columnList = "chat_id"),
    @Index(name = "idx_csi_sender_id", columnList = "sender_id"),
    @Index(name = "idx_csi_sent_at",   columnList = "sent_at"),
    @Index(name = "idx_csi_media_type",columnList = "media_type")
})
public class ChatSearchIndex {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "sender_id", nullable = false)
    private UUID senderId;

    // Added: for searching by sender username
    @Column(name = "sender_username")
    private String senderUsername;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(name = "file_name")
    private String fileName;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(name = "media_type")
    private String mediaType;

    @Column(name = "sent_at", nullable = false)
    private Instant sentAt;

    // Added: date-only field for date-based grouping and search
    @Column(name = "message_date", nullable = false)
    private LocalDate messageDate;

    public UUID getId() { return id; }
    public UUID getChatId() { return chatId; }
    public void setChatId(UUID chatId) { this.chatId = chatId; }
    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID senderId) { this.senderId = senderId; }
    public String getSenderUsername() { return senderUsername; }
    public void setSenderUsername(String senderUsername) { this.senderUsername = senderUsername; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getMediaType() { return mediaType; }
    public void setMediaType(String mediaType) { this.mediaType = mediaType; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public LocalDate getMessageDate() { return messageDate; }
    public void setMessageDate(LocalDate messageDate) { this.messageDate = messageDate; }
}
