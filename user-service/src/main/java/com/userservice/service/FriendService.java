package com.userservice.service;

import com.userservice.dto.UserDtos.*;
import java.util.List;
import java.util.UUID;

public interface FriendService {

    List<FriendSummaryResponse> listFriends(
            UUID loggedInUserId,
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