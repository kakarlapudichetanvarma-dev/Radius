package com.chatservice.dto;

import com.chatservice.entity.CommunityJoinRequest;
import com.chatservice.entity.CommunityMember;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class CommunityDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateCommunityRequest {
        private String name;
        private String description;
        private String photoUrl;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AddCommunityMemberRequest {
        private UUID userId;
        private CommunityMember.Role role;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LinkGroupRequest {
        private UUID groupId;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GenerateInviteRequest {
        private Integer expiryHours;
        private Integer maxUses;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CommunityResponse {
        private UUID id;
        private String name;
        private String description;
        private String photoUrl;
        private UUID createdBy;
        private LocalDateTime createdAt;
        private int memberCount;
        private int groupCount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CommunityGroupSummary {
        private UUID groupId;
        private UUID chatId;
        private String groupName;
        private String groupPhotoUrl;
        private int memberCount;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CommunityMemberPhoneView {
        private UUID userId;
        private String name;
        private String phoneNumber;
        private String avatarUrl;
        private CommunityMember.Role role;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CommunityMemberResponse {
        private UUID id;
        private UUID userId;
        private String name;
        private CommunityMember.Role role;
        private LocalDateTime joinedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CommunityMembersResponse {
        private List<CommunityMemberResponse> members;
        private CommunityMember.Role currentUserRole;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class CreateGroupInCommunityRequest {
        private String name;
        private List<UUID> memberIds;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class GenerateInviteResponse {
        private String token;
        private String inviteLink;
        private LocalDateTime expiresAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class InvitePreviewResponse {
        private UUID communityId;
        private String communityName;
        private String communityDescription;
        private String communityPhotoUrl;
        private int memberCount;
        private int groupCount;
        private String createdByName;
        private boolean isValid;
        private String invalidReason;
    }

    // ── Join Requests ─────────────────────────────────────────────────────────

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class JoinRequestResponse {
        private UUID id;
        private UUID communityId;
        private UUID userId;
        private String userName;
        private CommunityJoinRequest.Status status;
        private LocalDateTime requestedAt;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class ReviewJoinRequestRequest {
        private boolean accept;
    }
}