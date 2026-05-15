package com.chatservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

// ─────────────────────────────────────────────────────────────────────────────
// GroupEvent
// ─────────────────────────────────────────────────────────────────────────────
@Entity
@Table(name = "group_events")
public class GroupEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "event_type", nullable = false)
    private String eventType; // MEMBER_JOINED, MEMBER_LEFT, etc.

    @Column(name = "actor_id", nullable = false)
    private UUID actorId;

    @Column(name = "target_id")
    private UUID targetId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @PrePersist
    void prePersist() { if (occurredAt == null) occurredAt = Instant.now(); }

    public UUID getId() { return id; }
    public UUID getGroupId() { return groupId; }
    public void setGroupId(UUID groupId) { this.groupId = groupId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public UUID getActorId() { return actorId; }
    public void setActorId(UUID actorId) { this.actorId = actorId; }
    public UUID getTargetId() { return targetId; }
    public void setTargetId(UUID targetId) { this.targetId = targetId; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getOccurredAt() { return occurredAt; }
}