package com.chatservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_invites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false, unique = true)
    private String token;

    private LocalDateTime expiresAt;

    @Builder.Default
    @Column(name = "is_active")
    private boolean isActive = true;

    @Builder.Default
    @Column(name = "use_count")
    private int useCount = 0;

    @Column(name = "max_uses")
    private Integer maxUses;

    @CreationTimestamp
    private LocalDateTime createdAt;
}