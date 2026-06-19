package com.chatservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────────────────────
// MessageStar — per-user star on a message (private, like WhatsApp).
// One row per (messageId, userId) pair.
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "message_stars",
       uniqueConstraints = @UniqueConstraint(columnNames = {"message_id", "user_id"}),
       indexes = {
           @Index(name = "idx_star_message_id", columnList = "message_id"),
           @Index(name = "idx_star_user_id",    columnList = "user_id")
       })
public class MessageStar {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Stored redundantly so "get all starred messages for user" doesn't need
    // to join through messages just to know which chat to navigate to.
    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @Column(name = "starred_at", nullable = false)
    private Instant starredAt;

    @PrePersist
    void prePersist() { if (starredAt == null) starredAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getChatId() { return chatId; }
    public void setChatId(UUID chatId) { this.chatId = chatId; }
    public Instant getStarredAt() { return starredAt; }
}