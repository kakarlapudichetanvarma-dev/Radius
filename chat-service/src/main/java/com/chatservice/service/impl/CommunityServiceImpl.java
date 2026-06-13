package com.chatservice.service.impl;

import com.chatservice.config.UserServiceClient;
import com.chatservice.dto.CommunityDtos.*;
import com.chatservice.entity.*;
import com.chatservice.entity.CommunityJoinRequest.Status;
import com.chatservice.entity.CommunityMember.Role;
import com.chatservice.exception.ChatExceptions.*;
import com.chatservice.repository.*;
import com.chatservice.service.CommunityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommunityServiceImpl implements CommunityService {

    private final CommunityRepository communityRepository;
    private final CommunityMemberRepository communityMemberRepository;
    private final CommunityGroupLinkRepository communityGroupLinkRepository;
    private final CommunityInviteRepository communityInviteRepository;
    private final CommunityJoinRequestRepository communityJoinRequestRepository;
    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserServiceClient userServiceClient;
    private final ChatRepository chatRepository;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    // ── Community CRUD ────────────────────────────────────────────────────────

    @Override
    @Transactional
    public CommunityResponse createCommunity(UUID creatorUserId, CreateCommunityRequest request) {
        Community community = Community.builder()
                .name(request.getName())
                .description(request.getDescription())
                .photoUrl(request.getPhotoUrl())
                .createdBy(creatorUserId)
                .build();
        community = communityRepository.save(community);

        CommunityMember adminMember = CommunityMember.builder()
                .communityId(community.getId())
                .userId(creatorUserId)
                .role(Role.ADMIN)
                .build();
        communityMemberRepository.save(adminMember);

        log.info("Community created: {} by user {}", community.getId(), creatorUserId);
        return toCommunityResponse(community);
    }

    @Override
    public List<CommunityResponse> getMyCommunities(UUID userId) {
        return communityMemberRepository.findByUserId(userId).stream()
                .map(m -> communityRepository.findById(m.getCommunityId()).orElse(null))
                .filter(c -> c != null && c.isActive())
                .map(this::toCommunityResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CommunityResponse getCommunityById(UUID communityId, UUID requestingUserId) {
        Community community = findCommunityOrThrow(communityId);
        assertIsMember(communityId, requestingUserId);
        return toCommunityResponse(community);
    }

    @Override
    @Transactional
    public void deleteCommunity(UUID communityId, UUID adminUserId) {
        assertIsAdmin(communityId, adminUserId);
        Community community = findCommunityOrThrow(communityId);
        community.setActive(false);
        communityRepository.save(community);
        log.info("Community {} soft-deleted by admin {}", communityId, adminUserId);
    }

    // ── Members ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void addMember(UUID communityId, UUID adminUserId, AddCommunityMemberRequest request) {
        assertIsAdmin(communityId, adminUserId);
        if (communityMemberRepository.existsByCommunityIdAndUserId(communityId, request.getUserId())) {
            throw new AlreadyMemberException("User is already a member of this community");
        }
        CommunityMember member = CommunityMember.builder()
                .communityId(communityId)
                .userId(request.getUserId())
                .role(request.getRole() != null ? request.getRole() : Role.MEMBER)
                .build();
        communityMemberRepository.save(member);
        log.info("User {} added to community {} by admin {}", request.getUserId(), communityId, adminUserId);
    }

    @Override
    @Transactional
    public void removeMember(UUID communityId, UUID adminUserId, UUID targetUserId) {
        assertIsAdmin(communityId, adminUserId);
        if (!communityMemberRepository.existsByCommunityIdAndUserId(communityId, targetUserId)) {
            throw new NotChatMemberException("User is not a member of this community");
        }
        communityMemberRepository.deleteByCommunityIdAndUserId(communityId, targetUserId);
        log.info("User {} removed from community {} by admin {}", targetUserId, communityId, adminUserId);
    }

    @Override
    @Transactional
    public void leaveCommunity(UUID communityId, UUID userId) {
        CommunityMember member = communityMemberRepository
                .findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new NotChatMemberException("You are not a member of this community"));

        if (member.getRole() == Role.ADMIN) {
            List<CommunityMember> allMembers = communityMemberRepository.findByCommunityId(communityId);
            long adminCount = allMembers.stream().filter(m -> m.getRole() == Role.ADMIN).count();
            if (adminCount <= 1 && allMembers.size() > 1) {
                throw new NotGroupAdminException(
                        "You are the only admin. Promote another member to admin before leaving.");
            }
        }

        communityMemberRepository.deleteByCommunityIdAndUserId(communityId, userId);
        log.info("User {} left community {}", userId, communityId);
    }

    @Override
    public CommunityMembersResponse getCommunityMembers(UUID communityId, UUID requestingUserId) {
        findCommunityOrThrow(communityId);

        CommunityMember requestingMember = communityMemberRepository
                .findByCommunityIdAndUserId(communityId, requestingUserId)
                .orElseThrow(() -> new NotChatMemberException("You are not a member of this community"));

        List<CommunityMember> members = communityMemberRepository.findByCommunityId(communityId);

        List<CommunityMemberResponse> memberResponses = members.stream()
                .map(m -> {
                    String name = resolveName(m.getUserId());
                    return CommunityMemberResponse.builder()
                            .id(m.getId())
                            .userId(m.getUserId())
                            .name(name)
                            .role(m.getRole())
                            .joinedAt(m.getJoinedAt())
                            .build();
                })
                .collect(Collectors.toList());

        return CommunityMembersResponse.builder()
                .members(memberResponses)
                .currentUserRole(requestingMember.getRole())
                .build();
    }

    // ── Groups ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void linkGroup(UUID communityId, UUID adminUserId, LinkGroupRequest request) {
        assertIsAdmin(communityId, adminUserId);
        if (!groupRepository.existsById(request.getGroupId())) {
            throw new GroupNotFoundException("Group not found: " + request.getGroupId());
        }
        if (communityGroupLinkRepository.existsByCommunityIdAndGroupId(communityId, request.getGroupId())) {
            throw new AlreadyMemberException("Group is already linked to this community");
        }
        CommunityGroupLink link = CommunityGroupLink.builder()
                .communityId(communityId)
                .groupId(request.getGroupId())
                .addedBy(adminUserId)
                .build();
        communityGroupLinkRepository.save(link);
    }

    @Override
    @Transactional
    public CommunityGroupSummary createGroupInCommunity(UUID communityId, UUID requestingUserId,
                                                         CreateGroupInCommunityRequest request) {
        findCommunityOrThrow(communityId);
        assertIsMember(communityId, requestingUserId);

        Chat chat = new Chat();
        chat.setType(Chat.ChatType.GROUP);
        chat = chatRepository.save(chat);

        Group group = new Group();
        group.setName(request.getName());
        group.setChatId(chat.getId());
        group.setCreatorId(requestingUserId);
        group.setMemberCount(1);
        group = groupRepository.save(group);

        GroupMember creatorMember = new GroupMember();
        creatorMember.setGroupId(group.getId());
        creatorMember.setUserId(requestingUserId);
        creatorMember.setUsername(resolveName(requestingUserId));
        creatorMember.setRole(GroupMember.Role.ADMIN);
        groupMemberRepository.save(creatorMember);

        if (request.getMemberIds() != null) {
            for (UUID memberId : request.getMemberIds()) {
                if (memberId.equals(requestingUserId)) continue;
                if (communityMemberRepository.existsByCommunityIdAndUserId(communityId, memberId)) {
                    GroupMember gm = new GroupMember();
                    gm.setGroupId(group.getId());
                    gm.setUserId(memberId);
                    gm.setUsername(resolveName(memberId));
                    gm.setRole(GroupMember.Role.MEMBER);
                    groupMemberRepository.save(gm);
                }
            }
        }

        int memberCount = groupMemberRepository.findByGroupId(group.getId()).size();
        group.setMemberCount(memberCount);
        group = groupRepository.save(group);

        CommunityGroupLink link = CommunityGroupLink.builder()
                .communityId(communityId)
                .groupId(group.getId())
                .addedBy(requestingUserId)
                .build();
        communityGroupLinkRepository.save(link);

        log.info("Group {} created inside community {} by user {}", group.getId(), communityId, requestingUserId);

        return CommunityGroupSummary.builder()
                .groupId(group.getId())
                .chatId(group.getChatId())
                .groupName(group.getName())
                .groupPhotoUrl(null)
                .memberCount(memberCount)
                .build();
    }

    @Override
    @Transactional
    public void unlinkGroup(UUID communityId, UUID adminUserId, UUID groupId) {
        assertIsAdmin(communityId, adminUserId);
        communityGroupLinkRepository.deleteByCommunityIdAndGroupId(communityId, groupId);
    }

    @Override
    public List<CommunityGroupSummary> getCommunityGroups(UUID communityId, UUID requestingUserId) {
        assertIsMember(communityId, requestingUserId);
        return communityGroupLinkRepository.findByCommunityIdAndIsVisibleTrue(communityId).stream()
                .map(link -> {
                    Group group = groupRepository.findById(link.getGroupId()).orElse(null);
                    if (group == null) return null;
                    int memberCount = groupMemberRepository.findByGroupId(link.getGroupId()).size();
                    return CommunityGroupSummary.builder()
                            .groupId(group.getId())
                            .chatId(group.getChatId())
                            .groupName(group.getName())
                            .groupPhotoUrl(null)
                            .memberCount(memberCount)
                            .build();
                })
                .filter(s -> s != null)
                .collect(Collectors.toList());
    }

    @Override
    public List<CommunityMemberPhoneView> getGroupMembers(UUID communityId, UUID groupId, UUID requestingUserId) {
        assertIsMember(communityId, requestingUserId);
        communityGroupLinkRepository.findByCommunityIdAndGroupId(communityId, groupId)
                .orElseThrow(() -> new GroupNotFoundException("Group is not part of this community"));

        return groupMemberRepository.findByGroupId(groupId).stream().map(gm ->
                CommunityMemberPhoneView.builder()
                        .userId(gm.getUserId())
                        .name(resolveName(gm.getUserId()))
                        .phoneNumber("")
                        .avatarUrl(null)
                        .role(communityMemberRepository
                                .findByCommunityIdAndUserId(communityId, gm.getUserId())
                                .map(CommunityMember::getRole)
                                .orElse(null))
                        .build()
        ).collect(Collectors.toList());
    }

    // ── Invite ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public GenerateInviteResponse generateInviteLink(UUID communityId, UUID requestingUserId,
                                                      GenerateInviteRequest request) {
        assertIsMember(communityId, requestingUserId);
        findCommunityOrThrow(communityId);

        String token = UUID.randomUUID().toString().replace("-", "")
                + Long.toHexString(System.currentTimeMillis());

        LocalDateTime expiresAt = null;
        if (request.getExpiryHours() != null) {
            expiresAt = LocalDateTime.now().plusHours(request.getExpiryHours());
        }

        CommunityInvite invite = CommunityInvite.builder()
                .communityId(communityId)
                .createdBy(requestingUserId)
                .token(token)
                .expiresAt(expiresAt)
                .maxUses(request.getMaxUses())
                .build();
        communityInviteRepository.save(invite);

        // Uses configurable base URL instead of hardcoded domain
        String inviteLink = baseUrl + "/community/invite/" + token;
        log.info("Invite generated for community {} by user {}", communityId, requestingUserId);

        return GenerateInviteResponse.builder()
                .token(token)
                .inviteLink(inviteLink)
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public InvitePreviewResponse getInvitePreview(String token) {
        CommunityInvite invite = communityInviteRepository
                .findByTokenAndIsActiveTrue(token).orElse(null);

        if (invite == null) {
            return InvitePreviewResponse.builder().isValid(false).invalidReason("REVOKED").build();
        }
        if (invite.getExpiresAt() != null && LocalDateTime.now().isAfter(invite.getExpiresAt())) {
            return InvitePreviewResponse.builder().isValid(false).invalidReason("EXPIRED").build();
        }
        if (invite.getMaxUses() != null && invite.getUseCount() >= invite.getMaxUses()) {
            return InvitePreviewResponse.builder().isValid(false).invalidReason("MAX_USES_REACHED").build();
        }

        Community community = findCommunityOrThrow(invite.getCommunityId());
        int memberCount = communityMemberRepository.findByCommunityId(community.getId()).size();
        int groupCount = communityGroupLinkRepository
                .findByCommunityIdAndIsVisibleTrue(community.getId()).size();

        return InvitePreviewResponse.builder()
                .communityId(community.getId())
                .communityName(community.getName())
                .communityDescription(community.getDescription())
                .communityPhotoUrl(community.getPhotoUrl())
                .memberCount(memberCount)
                .groupCount(groupCount)
                .createdByName(resolveName(community.getCreatedBy()))
                .isValid(true)
                .build();
    }

    @Override
    @Transactional
    public void joinViaInvite(String token, UUID userId) {
        CommunityInvite invite = communityInviteRepository.findByTokenAndIsActiveTrue(token)
                .orElseThrow(() -> new ChatNotFoundException("Invite link is invalid or revoked"));

        if (invite.getExpiresAt() != null && LocalDateTime.now().isAfter(invite.getExpiresAt())) {
            throw new ChatNotFoundException("This invite link has expired");
        }
        if (invite.getMaxUses() != null && invite.getUseCount() >= invite.getMaxUses()) {
            throw new ChatNotFoundException("This invite link has reached its maximum uses");
        }
        if (communityMemberRepository.existsByCommunityIdAndUserId(invite.getCommunityId(), userId)) {
            log.info("User {} already a member of community {}", userId, invite.getCommunityId());
            return;
        }

        communityMemberRepository.save(CommunityMember.builder()
                .communityId(invite.getCommunityId())
                .userId(userId)
                .role(Role.MEMBER)
                .build());

        invite.setUseCount(invite.getUseCount() + 1);
        communityInviteRepository.save(invite);
        log.info("User {} joined community {} via invite", userId, invite.getCommunityId());
    }

    // ── Join Requests ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void requestToJoin(UUID communityId, UUID userId) {
        findCommunityOrThrow(communityId);

        if (communityMemberRepository.existsByCommunityIdAndUserId(communityId, userId)) {
            throw new AlreadyMemberException("You are already a member of this community");
        }

        if (communityJoinRequestRepository.existsByCommunityIdAndUserIdAndStatus(
                communityId, userId, Status.PENDING)) {
            throw new AlreadyMemberException("You already have a pending join request for this community");
        }

        CommunityJoinRequest joinRequest = CommunityJoinRequest.builder()
                .communityId(communityId)
                .userId(userId)
                .status(Status.PENDING)
                .build();
        communityJoinRequestRepository.save(joinRequest);
        log.info("User {} requested to join community {}", userId, communityId);
    }

    @Override
    @Transactional
    public void reviewJoinRequest(UUID communityId, UUID requestId, UUID adminUserId, boolean accept) {
        assertIsAdmin(communityId, adminUserId);

        CommunityJoinRequest joinRequest = communityJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new ChatNotFoundException("Join request not found"));

        if (!joinRequest.getCommunityId().equals(communityId)) {
            throw new ChatNotFoundException("Join request does not belong to this community");
        }

        if (joinRequest.getStatus() != Status.PENDING) {
            throw new AlreadyMemberException("This join request has already been reviewed");
        }

        joinRequest.setStatus(accept ? Status.ACCEPTED : Status.REJECTED);
        joinRequest.setReviewedBy(adminUserId);
        joinRequest.setReviewedAt(LocalDateTime.now());
        communityJoinRequestRepository.save(joinRequest);

        if (accept) {
            if (!communityMemberRepository.existsByCommunityIdAndUserId(communityId, joinRequest.getUserId())) {
                communityMemberRepository.save(CommunityMember.builder()
                        .communityId(communityId)
                        .userId(joinRequest.getUserId())
                        .role(Role.MEMBER)
                        .build());
                log.info("Join request accepted: user {} joined community {}", joinRequest.getUserId(), communityId);
            }
        } else {
            log.info("Join request rejected: user {} denied from community {}", joinRequest.getUserId(), communityId);
        }
    }

    @Override
    public List<JoinRequestResponse> getPendingJoinRequests(UUID communityId, UUID adminUserId) {
        assertIsAdmin(communityId, adminUserId);

        return communityJoinRequestRepository
                .findByCommunityIdAndStatus(communityId, Status.PENDING)
                .stream()
                .map(r -> JoinRequestResponse.builder()
                        .id(r.getId())
                        .communityId(r.getCommunityId())
                        .userId(r.getUserId())
                        .userName(resolveName(r.getUserId()))
                        .status(r.getStatus())
                        .requestedAt(r.getRequestedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Community findCommunityOrThrow(UUID communityId) {
        return communityRepository.findById(communityId)
                .orElseThrow(() -> new ChatNotFoundException("Community not found: " + communityId));
    }

    private void assertIsMember(UUID communityId, UUID userId) {
        if (!communityMemberRepository.existsByCommunityIdAndUserId(communityId, userId)) {
            throw new NotChatMemberException("You are not a member of this community");
        }
    }

    private void assertIsAdmin(UUID communityId, UUID userId) {
        CommunityMember member = communityMemberRepository
                .findByCommunityIdAndUserId(communityId, userId)
                .orElseThrow(() -> new NotChatMemberException("You are not a member of this community"));
        if (member.getRole() != Role.ADMIN) {
            throw new NotGroupAdminException("You are not an admin of this community");
        }
    }

    private String resolveName(UUID userId) {
        try {
            return userServiceClient.getUsernameById(userId);
        } catch (Exception e) {
            return "Unknown";
        }
    }

    private CommunityResponse toCommunityResponse(Community community) {
        int memberCount = communityMemberRepository.findByCommunityId(community.getId()).size();
        int groupCount = communityGroupLinkRepository
                .findByCommunityIdAndIsVisibleTrue(community.getId()).size();
        return CommunityResponse.builder()
                .id(community.getId())
                .name(community.getName())
                .description(community.getDescription())
                .photoUrl(community.getPhotoUrl())
                .createdBy(community.getCreatedBy())
                .createdAt(community.getCreatedAt())
                .memberCount(memberCount)
                .groupCount(groupCount)
                .build();
    }
}