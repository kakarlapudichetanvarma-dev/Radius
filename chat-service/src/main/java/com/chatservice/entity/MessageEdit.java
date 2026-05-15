package com.chatservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────────────────────
// MessageEdit — stores original content before each edit
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "message_edits")
public class MessageEdit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "message_id", nullable = false)
    private UUID messageId;

    @Column(name = "original_content", nullable = false, columnDefinition = "TEXT")
    private String originalContent;

    @Column(name = "edited_content", nullable = false, columnDefinition = "TEXT")
    private String editedContent;

    @Column(name = "edited_at", nullable = false)
    private Instant editedAt;

    @Column(name = "edited_by", nullable = false)
    private UUID editedBy;

    @PrePersist
    void prePersist() { if (editedAt == null) editedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    public String getOriginalContent() { return originalContent; }
    public void setOriginalContent(String originalContent) { this.originalContent = originalContent; }
    public String getEditedContent() { return editedContent; }
    public void setEditedContent(String editedContent) { this.editedContent = editedContent; }
    public Instant getEditedAt() { return editedAt; }
    public UUID getEditedBy() { return editedBy; }
    public void setEditedBy(UUID editedBy) { this.editedBy = editedBy; }
}