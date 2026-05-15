package com.chatservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "archived_chats",
       uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id", "user_id"}),
       indexes = {
           @Index(name = "idx_ac_user_id", columnList = "user_id"),
           @Index(name = "idx_ac_chat_id", columnList = "chat_id")
       })
public class ArchivedChat {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "archived_at", nullable = false)
    private Instant archivedAt;

    @PrePersist
    void prePersist() { if (archivedAt == null) archivedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getChatId() { return chatId; }
    public void setChatId(UUID chatId) { this.chatId = chatId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public Instant getArchivedAt() { return archivedAt; }
}
