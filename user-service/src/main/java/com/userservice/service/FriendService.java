package com.userservice.service;

import com.userservice.dto.UserDtos.*;
import java.util.List;
import java.util.UUID;

public interface FriendService {

    FriendRequestResponse sendFriendRequest(
            UUID requesterId,
            String targetPhoneNumber,
            String token);

    FriendRequestResponse respondToRequest(
            UUID receiverId,
            UUID requestId,
            boolean accept,
            String token);

    List<FriendSummaryResponse> listFriends(
            UUID loggedInUserId,
            String token);

    List<FriendRequestResponse> listPendingRequests(
            UUID userId,
            String token);

    UserSearchResult searchByPhone(
            UUID requesterId,
            String phone,
            String token);

    FriendSummaryResponse addDirectFriend(
            UUID requesterId,
            String phoneNumber,
            String token);

}