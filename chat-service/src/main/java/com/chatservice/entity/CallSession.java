package com.chatservice.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "call_sessions", indexes = {
    @Index(name = "idx_cs_chat_id",   columnList = "chat_id"),
    @Index(name = "idx_cs_caller_id", columnList = "caller_id"),
    @Index(name = "idx_cs_callee_id", columnList = "callee_id"),
    @Index(name = "idx_cs_status",    columnList = "call_status")
})
public class CallSession {

    public enum CallType   { AUDIO, VIDEO }
    public enum CallStatus { INITIATED, RINGING, ACTIVE, ENDED, MISSED, DECLINED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "chat_id", nullable = false)
    private UUID chatId;

    @Column(name = "caller_id", nullable = false)
    private UUID callerId;

    @Column(name = "callee_id", nullable = false)
    private UUID calleeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_type", nullable = false)
    private CallType callType;

    @Enumerated(EnumType.STRING)
    @Column(name = "call_status", nullable = false)
    private CallStatus callStatus = CallStatus.INITIATED;

    // FIX: startedAt is NOT set in @PrePersist anymore.
    // It is only set explicitly in service when status transitions to ACTIVE.
    // INITIATED and RINGING calls should NOT have a startedAt.
    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "answered_at")
    private Instant answeredAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "duration_seconds")
    private Long durationSeconds;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void prePersist() {
        // Only set createdAt here — startedAt is set by service when ACTIVE
        createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getChatId() { return chatId; }
    public void setChatId(UUID chatId) { this.chatId = chatId; }
    public UUID getCallerId() { return callerId; }
    public void setCallerId(UUID callerId) { this.callerId = callerId; }
    public UUID getCalleeId() { return calleeId; }
    public void setCalleeId(UUID calleeId) { this.calleeId = calleeId; }
    public CallType getCallType() { return callType; }
    public void setCallType(CallType callType) { this.callType = callType; }
    public CallStatus getCallStatus() { return callStatus; }
    public void setCallStatus(CallStatus callStatus) { this.callStatus = callStatus; }
    public Instant getStartedAt() { return startedAt; }
    public void setStartedAt(Instant startedAt) { this.startedAt = startedAt; }
    public Instant getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(Instant answeredAt) { this.answeredAt = answeredAt; }
    public Instant getEndedAt() { return endedAt; }
    public void setEndedAt(Instant endedAt) { this.endedAt = endedAt; }
    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
    public Instant getCreatedAt() { return createdAt; }
}
