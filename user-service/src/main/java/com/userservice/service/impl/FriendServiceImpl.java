package com.userservice.service.impl;

import com.userservice.client.AuthServiceClient;
import com.userservice.dto.AuthApiResponse;
import com.userservice.dto.AuthUserResponse;
import com.userservice.dto.UserDtos.*;
import com.userservice.entity.Friend;
import com.userservice.exception.UserExceptions.*;
import com.userservice.repository.FriendRepository;
import com.userservice.service.FriendService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {

    private static final Logger log =
            Logger.getLogger(FriendServiceImpl.class.getName());

    private final FriendRepository      friendRepository;
    private final AuthServiceClient     authServiceClient;

    public FriendServiceImpl(
            FriendRepository friendRepository,
            AuthServiceClient authServiceClient) {

        this.friendRepository  = friendRepository;
        this.authServiceClient = authServiceClient;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List friends
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public List<FriendSummaryResponse> listFriends(UUID userId, String token) {

        return friendRepository
                .findAllByUserId(userId)
                .stream()
                .map(f -> {

                    FriendSummaryResponse response = new FriendSummaryResponse();
                    response.setUserId(f.getFriendId().toString());
                    response.setFriendsSince(f.getSince().toString());
                    response.setUsername(f.getUsername());
                    response.setEmail(f.getEmail());
                    response.setPhoneNumber(f.getPhoneNumber());
                    response.setProfilePicture(f.getProfilePicture());

                    // Refresh from auth-service for latest profile data
                    try {
                        AuthApiResponse apiResponse =
                                authServiceClient.getUserById(f.getFriendId(), token);

                        if (apiResponse != null
                                && apiResponse.isSuccess()
                                && apiResponse.getData() != null) {

                            AuthUserResponse auth = apiResponse.getData();
                            response.setUsername(auth.getUsername());
                            response.setEmail(auth.getEmail());
                            response.setPhoneNumber(auth.getPhoneNumber());
                            response.setProfilePicture(auth.getProfilePicture());
                        }

                    } catch (Exception ex) {
                        log.warning(
                            "Failed to fetch friend profile for "
                            + f.getFriendId() + ": " + ex.getMessage()
                        );
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search by phone
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    public UserSearchResult searchByPhone(UUID requesterId, String phone, String token) {

        AuthApiResponse apiResponse = authServiceClient.getUserByPhone(phone, token);

        if (apiResponse == null
                || !apiResponse.isSuccess()
                || apiResponse.getData() == null) {
            throw new RuntimeException("No user found with phone number: " + phone);
        }

        AuthUserResponse found = apiResponse.getData();
        UUID foundId = UUID.fromString(found.getId());

        if (foundId.equals(requesterId)) {
            throw new RuntimeException("That is your own number.");
        }

        boolean alreadyFriend =
                friendRepository.existsByUserIdAndFriendId(requesterId, foundId);

        UserSearchResult result = new UserSearchResult();
        result.setUserId(found.getId());
        result.setUsername(found.getUsername());
        result.setPhoneNumber(found.getPhoneNumber());
        result.setProfilePicture(found.getProfilePicture());
        result.setAlreadyFriend(alreadyFriend);

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Add friend directly
    // ─────────────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public FriendSummaryResponse addDirectFriend(
            UUID requesterId,
            String phoneNumber,
            String token) {

        AuthApiResponse apiResponse =
                authServiceClient.getUserByPhone(phoneNumber, token);

        if (apiResponse == null
                || !apiResponse.isSuccess()
                || apiResponse.getData() == null) {
            throw new RuntimeException(
                "No user found with phone number: " + phoneNumber);
        }

        AuthUserResponse targetUser = apiResponse.getData();
        UUID targetUserId = UUID.fromString(targetUser.getId());

        if (targetUserId.equals(requesterId)) {
            throw new RuntimeException("You cannot add yourself.");
        }

        if (friendRepository.existsByUserIdAndFriendId(requesterId, targetUserId)) {
            throw new RuntimeException("Already friends.");
        }

        AuthUserResponse requesterAuth = getRequesterAuth(requesterId, token);

        LocalDateTime now = LocalDateTime.now();

        // Bidirectional friendship
        Friend friend1 = new Friend();
        friend1.setUserId(requesterId);
        friend1.setFriendId(targetUserId);
        friend1.setUsername(targetUser.getUsername());
        friend1.setEmail(targetUser.getEmail());
        friend1.setPhoneNumber(targetUser.getPhoneNumber());
        friend1.setProfilePicture(targetUser.getProfilePicture());
        friend1.setSince(now);
        friendRepository.save(friend1);

        Friend friend2 = new Friend();
        friend2.setUserId(targetUserId);
        friend2.setFriendId(requesterId);
        friend2.setSince(now);

        if (requesterAuth != null) {
            friend2.setUsername(requesterAuth.getUsername());
            friend2.setEmail(requesterAuth.getEmail());
            friend2.setPhoneNumber(requesterAuth.getPhoneNumber());
            friend2.setProfilePicture(requesterAuth.getProfilePicture());
        }

        friendRepository.save(friend2);

        FriendSummaryResponse response = new FriendSummaryResponse();
        response.setUserId(targetUser.getId());
        response.setUsername(targetUser.getUsername());
        response.setEmail(targetUser.getEmail());
        response.setPhoneNumber(targetUser.getPhoneNumber());
        response.setProfilePicture(targetUser.getProfilePicture());
        response.setFriendsSince(now.toString());

        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private AuthUserResponse getRequesterAuth(UUID userId, String token) {

        int maxRetries = 3;

        for (int i = 0; i < maxRetries; i++) {
            try {
                AuthApiResponse apiResponse =
                        authServiceClient.getUserById(userId, token);

                if (apiResponse != null
                        && apiResponse.isSuccess()
                        && apiResponse.getData() != null) {
                    return apiResponse.getData();
                }

            } catch (Exception ex) {
                log.warning(
                    "getRequesterAuth attempt " + (i + 1)
                    + " failed for userId=" + userId + ": " + ex.getMessage()
                );

                if (i < maxRetries - 1) {
                    try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                }
            }
        }

        log.warning("getRequesterAuth: all retries failed for userId=" + userId);
        return null;
    }
}