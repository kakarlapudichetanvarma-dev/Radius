package com.chatservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "chat_settings",
        uniqueConstraints = @UniqueConstraint(columnNames = {"chat_id"})
)
public class ChatSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chat_id", nullable = false, unique = true)
    private UUID chatId;

    // Base64 encoded wallpaper image OR a wallpaper theme name
    @Column(name = "wallpaper_data", columnDefinition = "TEXT")
    private String wallpaperData;

    // e.g. "IMAGE", "COLOR", "DEFAULT", "PATTERN", "GRADIENT"
    @Column(name = "wallpaper_type")
    private String wallpaperType;

    // Hex color or gradient string
    @Column(name = "wallpaper_color", columnDefinition = "TEXT")
    private String wallpaperColor;

    @Column(name = "muted", nullable = false)
    private boolean muted = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    void prePersist() {
        createdAt = updatedAt = Instant.now();
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public UUID getChatId() {
        return chatId;
    }

    public void setChatId(UUID chatId) {
        this.chatId = chatId;
    }

    public String getWallpaperData() {
        return wallpaperData;
    }

    public void setWallpaperData(String wallpaperData) {
        this.wallpaperData = wallpaperData;
    }

    public String getWallpaperType() {
        return wallpaperType;
    }

    public void setWallpaperType(String wallpaperType) {
        this.wallpaperType = wallpaperType;
    }

    public String getWallpaperColor() {
        return wallpaperColor;
    }

    public void setWallpaperColor(String wallpaperColor) {
        this.wallpaperColor = wallpaperColor;
    }

    public boolean isMuted() {
        return muted;
    }

    public void setMuted(boolean muted) {
        this.muted = muted;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}