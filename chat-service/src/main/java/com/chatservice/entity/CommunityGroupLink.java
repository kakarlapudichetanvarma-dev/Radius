package com.chatservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "community_group_links",
        uniqueConstraints = @UniqueConstraint(columnNames = {"community_id", "group_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommunityGroupLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "community_id", nullable = false)
    private UUID communityId;

    @Column(name = "group_id", nullable = false)
    private UUID groupId;

    @Column(name = "added_by", nullable = false)
    private UUID addedBy;

    @Builder.Default
    @Column(name = "is_visible")
    private boolean isVisible = true;

    @CreationTimestamp
    private LocalDateTime addedAt;
}