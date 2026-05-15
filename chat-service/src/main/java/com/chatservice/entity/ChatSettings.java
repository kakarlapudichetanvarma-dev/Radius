package com.chatservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chat_settings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id", "user_id"}))
public class ChatSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    // Base64 encoded wallpaper image OR a wallpaper theme name
    @Column(name = "wallpaper_data", columnDefinition = "TEXT")
    private String wallpaperData;

    // e.g. "IMAGE", "COLOR", "DEFAULT"
    @Column(name = "wallpaper_type")
    private String wallpaperType;

    // Hex color if type is COLOR e.g. "#1a1a2e"
    @Column(name = "wallpaper_color")
    private String wallpaperColor;

    @Column(name = "muted", nullable = false)
    private boolean muted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() { createdAt = updatedAt = Instant.now(); }

    @PreUpdate
    void preUpdate() { updatedAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getChatId() { return chatId; }
    public void setChatId(UUID chatId) { this.chatId = chatId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getWallpaperData() { return wallpaperData; }
    public void setWallpaperData(String wallpaperData) { this.wallpaperData = wallpaperData; }
    public String getWallpaperType() { return wallpaperType; }
    public void setWallpaperType(String wallpaperType) { this.wallpaperType = wallpaperType; }
    public String getWallpaperColor() { return wallpaperColor; }
    public void setWallpaperColor(String wallpaperColor) { this.wallpaperColor = wallpaperColor; }
    public boolean isMuted() { return muted; }
    public void setMuted(boolean muted) { this.muted = muted; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
