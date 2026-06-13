package com.chatservice.service;

import com.chatservice.dto.CommunityDtos.*;

import java.util.List;
import java.util.UUID;

public interface CommunityService {

    CommunityResponse createCommunity(UUID creatorUserId, CreateCommunityRequest request);

    List<CommunityResponse> getMyCommunities(UUID userId);

    CommunityResponse getCommunityById(UUID communityId, UUID requestingUserId);

    void addMember(UUID communityId, UUID adminUserId, AddCommunityMemberRequest request);

    void removeMember(UUID communityId, UUID adminUserId, UUID targetUserId);

    void leaveCommunity(UUID communityId, UUID userId);

    void linkGroup(UUID communityId, UUID adminUserId, LinkGroupRequest request);

    void unlinkGroup(UUID communityId, UUID adminUserId, UUID groupId);

    CommunityGroupSummary createGroupInCommunity(UUID communityId, UUID requestingUserId, CreateGroupInCommunityRequest request);

    List<CommunityGroupSummary> getCommunityGroups(UUID communityId, UUID requestingUserId);

    List<CommunityMemberPhoneView> getGroupMembers(UUID communityId, UUID groupId, UUID requestingUserId);

    CommunityMembersResponse getCommunityMembers(UUID communityId, UUID requestingUserId);

    void deleteCommunity(UUID communityId, UUID adminUserId);

    GenerateInviteResponse generateInviteLink(UUID communityId, UUID requestingUserId, GenerateInviteRequest request);

    InvitePreviewResponse getInvitePreview(String token);

    void joinViaInvite(String token, UUID userId);

    // ── Join Requests ──────────────────────────────────────────────────────────

    /** Any logged-in user requests to join a community they are not yet a member of */
    void requestToJoin(UUID communityId, UUID userId);

    /** Admin reviews (accept or reject) a pending join request */
    void reviewJoinRequest(UUID communityId, UUID requestId, UUID adminUserId, boolean accept);

    /** Admin fetches all pending join requests for a community */
    List<JoinRequestResponse> getPendingJoinRequests(UUID communityId, UUID adminUserId);
}