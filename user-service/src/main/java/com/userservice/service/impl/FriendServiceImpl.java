package com.userservice.service.impl;

import com.userservice.client.AuthServiceClient;
import com.userservice.dto.AuthApiResponse;
import com.userservice.dto.AuthUserResponse;
import com.userservice.dto.UserDtos.*;
import com.userservice.entity.Friend;
import com.userservice.entity.FriendRequest;
import com.userservice.entity.FriendRequest.Status;
import com.userservice.exception.UserExceptions.*;
import com.userservice.repository.FriendRepository;
import com.userservice.repository.FriendRequestRepository;
import com.userservice.service.FriendService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Service
public class FriendServiceImpl implements FriendService {

    private static final Logger log =
            Logger.getLogger(FriendServiceImpl.class.getName());

    private final FriendRequestRepository friendRequestRepository;
    private final FriendRepository friendRepository;
    private final AuthServiceClient authServiceClient;

    public FriendServiceImpl(
            FriendRequestRepository friendRequestRepository,
            FriendRepository friendRepository,
            AuthServiceClient authServiceClient) {

        this.friendRequestRepository = friendRequestRepository;
        this.friendRepository = friendRepository;
        this.authServiceClient = authServiceClient;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Send friend request
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public FriendRequestResponse sendFriendRequest(
            UUID requesterId,
            String targetPhoneNumber,
            String token) {

        AuthApiResponse receiverApiResponse =
                authServiceClient.getUserByPhone(
                        targetPhoneNumber,
                        token
                );

        if (receiverApiResponse == null
                || !receiverApiResponse.isSuccess()
                || receiverApiResponse.getData() == null) {

            throw new RuntimeException(
                    "Could not find user with phone number: "
                            + targetPhoneNumber);
        }

        AuthUserResponse receiverAuth =
                receiverApiResponse.getData();

        UUID receiverId =
                UUID.fromString(receiverAuth.getId());

        if (receiverId.equals(requesterId)) {
            throw new SelfFriendRequestException(
                    "You cannot send a friend request to yourself.");
        }

        if (friendRepository.existsByUserIdAndFriendId(
                requesterId,
                receiverId)) {

            throw new AlreadyFriendsException(
                    "You are already friends with this user.");
        }

        Optional<FriendRequest> existingOpt =
                friendRequestRepository.findBetweenWithStatuses(
                        requesterId,
                        receiverId,
                        List.of(
                                Status.PENDING,
                                Status.ACCEPTED,
                                Status.REJECTED
                        )
                );

        if (existingOpt.isPresent()) {

            FriendRequest existing = existingOpt.get();

            if (existing.getStatus() == Status.PENDING) {
                throw new FriendRequestAlreadyExistsException(
                        "A friend request is already pending.");
            }

            if (existing.getStatus() == Status.ACCEPTED) {
                throw new AlreadyFriendsException(
                        "You are already friends.");
            }

            if (existing.getStatus() == Status.REJECTED) {

                if (existing.getRequesterId().equals(requesterId)) {

                    AuthUserResponse requesterAuth =
                            getRequesterAuth(requesterId, token);

                    existing.setStatus(Status.PENDING);
                    existing.setRequesterUsername(
                            requesterAuth.getUsername()
                    );
                    existing.setEmail(requesterAuth.getEmail());
                    existing.setPhoneNumber(
                            requesterAuth.getPhoneNumber()
                    );

                    FriendRequest updated =
                            friendRequestRepository.save(existing);

                    return toRequestResponse(
                            updated,
                            requesterAuth
                    );
                }

                friendRequestRepository.delete(existing);
            }
        }

        AuthUserResponse requesterAuth =
                getRequesterAuth(requesterId, token);

        FriendRequest fr = new FriendRequest();

        fr.setRequesterId(requesterId);
        fr.setReceiverId(receiverId);
        fr.setRequesterUsername(requesterAuth.getUsername());
        fr.setEmail(requesterAuth.getEmail());
        fr.setPhoneNumber(requesterAuth.getPhoneNumber());
        fr.setStatus(Status.PENDING);

        fr = friendRequestRepository.save(fr);

        return toRequestResponse(fr, requesterAuth);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Respond to request
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public FriendRequestResponse respondToRequest(
            UUID receiverId,
            UUID requestId,
            boolean accept,
            String token) {

        FriendRequest fr =
                friendRequestRepository
                        .findByIdAndReceiverIdAndStatus(
                                requestId,
                                receiverId,
                                Status.PENDING
                        )
                        .orElseThrow(() ->
                                new FriendRequestNotFoundException(
                                        "No pending friend request found."
                                )
                        );

        if (accept) {

            fr.setStatus(Status.ACCEPTED);

            AuthUserResponse requesterAuth =
                    getRequesterAuth(fr.getRequesterId(), token);

            AuthUserResponse receiverAuth =
                    getRequesterAuth(fr.getReceiverId(), token);

            LocalDateTime now = LocalDateTime.now();

            Friend friend1 = new Friend();
            friend1.setUserId(fr.getRequesterId());
            friend1.setFriendId(fr.getReceiverId());
            friend1.setSince(now);

            if (receiverAuth != null) {
                friend1.setUsername(receiverAuth.getUsername());
                friend1.setEmail(receiverAuth.getEmail());
                friend1.setPhoneNumber(receiverAuth.getPhoneNumber());
                friend1.setProfilePicture(
                        receiverAuth.getProfilePicture()
                );
            }

            friendRepository.save(friend1);

            Friend friend2 = new Friend();
            friend2.setUserId(fr.getReceiverId());
            friend2.setFriendId(fr.getRequesterId());
            friend2.setSince(now);

            if (requesterAuth != null) {
                friend2.setUsername(requesterAuth.getUsername());
                friend2.setEmail(requesterAuth.getEmail());
                friend2.setPhoneNumber(requesterAuth.getPhoneNumber());
                friend2.setProfilePicture(
                        requesterAuth.getProfilePicture()
                );
            }

            friendRepository.save(friend2);

            fr = friendRequestRepository.save(fr);

            return toRequestResponse(fr, requesterAuth);

        } else {

            fr.setStatus(Status.REJECTED);

            fr = friendRequestRepository.save(fr);

            AuthUserResponse requesterAuth =
                    getRequesterAuth(fr.getRequesterId(), token);

            return toRequestResponse(fr, requesterAuth);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List friends
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<FriendSummaryResponse> listFriends(
            UUID userId,
            String token) {

        return friendRepository
                .findAllByUserId(userId)
                .stream()
                .map(f -> {

                    FriendSummaryResponse response =
                            new FriendSummaryResponse();

                    response.setUserId(
                            f.getFriendId().toString()
                    );

                    response.setFriendsSince(
                            f.getSince().toString()
                    );

                    response.setUsername(f.getUsername());
                    response.setEmail(f.getEmail());
                    response.setPhoneNumber(
                            f.getPhoneNumber()
                    );
                    response.setProfilePicture(
                            f.getProfilePicture()
                    );

                    try {

                        AuthApiResponse apiResponse =
                                authServiceClient.getUserById(
                                        f.getFriendId(),
                                        token
                                );

                        if (apiResponse != null
                                && apiResponse.isSuccess()
                                && apiResponse.getData() != null) {

                            AuthUserResponse auth =
                                    apiResponse.getData();

                            response.setUsername(auth.getUsername());
                            response.setEmail(auth.getEmail());
                            response.setPhoneNumber(
                                    auth.getPhoneNumber()
                            );
                            response.setProfilePicture(
                                    auth.getProfilePicture()
                            );
                        }

                    } catch (Exception ex) {

                        log.warning(
                                "Failed to fetch friend profile for "
                                        + f.getFriendId()
                                        + ": "
                                        + ex.getMessage()
                        );
                    }

                    return response;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List pending requests
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<FriendRequestResponse> listPendingRequests(
            UUID userId,
            String token) {

        return friendRequestRepository
                .findByReceiverIdAndStatus(
                        userId,
                        Status.PENDING
                )
                .stream()
                .map(fr -> {

                    FriendRequestResponse response =
                            new FriendRequestResponse();

                    response.setRequestId(fr.getId().toString());

                    response.setRequesterId(
                            fr.getRequesterId().toString()
                    );

                    response.setStatus(fr.getStatus().name());

                    response.setCreatedAt(
                            fr.getCreatedAt() != null
                                    ? fr.getCreatedAt().toString()
                                    : null
                    );

                    response.setRequesterUsername(
                            fr.getRequesterUsername()
                    );

                    response.setEmail(fr.getEmail());

                    response.setPhoneNumber(
                            fr.getPhoneNumber()
                    );

                    try {

                        AuthApiResponse apiResponse =
                                authServiceClient.getUserById(
                                        fr.getRequesterId(),
                                        token
                                );

                        if (apiResponse != null
                                && apiResponse.isSuccess()
                                && apiResponse.getData() != null) {

                            AuthUserResponse authUser =
                                    apiResponse.getData();

                            response.setRequesterUsername(
                                    authUser.getUsername()
                            );

                            response.setEmail(
                                    authUser.getEmail()
                            );

                            response.setPhoneNumber(
                                    authUser.getPhoneNumber()
                            );

                            response.setProfilePicture(
                                    authUser.getProfilePicture()
                            );
                        }

                    } catch (Exception ex) {

                        log.warning(
                                "Failed to fetch requester profile: "
                                        + ex.getMessage()
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
    public UserSearchResult searchByPhone(
            UUID requesterId,
            String phone,
            String token) {

        AuthApiResponse apiResponse =
                authServiceClient.getUserByPhone(phone, token);

        if (apiResponse == null
                || !apiResponse.isSuccess()
                || apiResponse.getData() == null) {

            throw new RuntimeException(
                    "No user found with phone number: " + phone);
        }

        AuthUserResponse found = apiResponse.getData();

        UUID foundId =
                UUID.fromString(found.getId());

        if (foundId.equals(requesterId)) {
            throw new RuntimeException(
                    "That is your own number.");
        }

        boolean alreadyFriend =
                friendRepository.existsByUserIdAndFriendId(
                        requesterId,
                        foundId
                );

        UserSearchResult result =
                new UserSearchResult();

        result.setUserId(found.getId());
        result.setUsername(found.getUsername());
        result.setPhoneNumber(found.getPhoneNumber());
        result.setProfilePicture(found.getProfilePicture());
        result.setAlreadyFriend(alreadyFriend);

        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DIRECT ADD FRIEND
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    @Transactional
    public FriendSummaryResponse addDirectFriend(
            UUID requesterId,
            String phoneNumber,
            String token) {

        AuthApiResponse apiResponse =
                authServiceClient.getUserByPhone(
                        phoneNumber,
                        token
                );

        if (apiResponse == null
                || !apiResponse.isSuccess()
                || apiResponse.getData() == null) {

            throw new RuntimeException(
                    "No user found with phone number: "
                            + phoneNumber);
        }

        AuthUserResponse targetUser =
                apiResponse.getData();

        UUID targetUserId =
                UUID.fromString(targetUser.getId());

        if (targetUserId.equals(requesterId)) {
            throw new RuntimeException(
                    "You cannot add yourself.");
        }

        boolean alreadyFriend =
                friendRepository.existsByUserIdAndFriendId(
                        requesterId,
                        targetUserId
                );

        if (alreadyFriend) {
            throw new RuntimeException(
                    "Already friends.");
        }

        AuthUserResponse requesterAuth =
                getRequesterAuth(requesterId, token);

        LocalDateTime now = LocalDateTime.now();

        Friend friend1 = new Friend();
        friend1.setUserId(requesterId);
        friend1.setFriendId(targetUserId);
        friend1.setUsername(targetUser.getUsername());
        friend1.setEmail(targetUser.getEmail());
        friend1.setPhoneNumber(targetUser.getPhoneNumber());
        friend1.setProfilePicture(
                targetUser.getProfilePicture()
        );
        friend1.setSince(now);

        friendRepository.save(friend1);

        Friend friend2 = new Friend();
        friend2.setUserId(targetUserId);
        friend2.setFriendId(requesterId);

        if (requesterAuth != null) {

            friend2.setUsername(
                    requesterAuth.getUsername()
            );

            friend2.setEmail(
                    requesterAuth.getEmail()
            );

            friend2.setPhoneNumber(
                    requesterAuth.getPhoneNumber()
            );

            friend2.setProfilePicture(
                    requesterAuth.getProfilePicture()
            );
        }

        friend2.setSince(now);

        friendRepository.save(friend2);

        FriendSummaryResponse response =
                new FriendSummaryResponse();

        response.setUserId(targetUser.getId());
        response.setUsername(targetUser.getUsername());
        response.setEmail(targetUser.getEmail());
        response.setPhoneNumber(targetUser.getPhoneNumber());
        response.setProfilePicture(
                targetUser.getProfilePicture()
        );

        response.setFriendsSince(now.toString());

        return response;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private AuthUserResponse getRequesterAuth(
            UUID userId,
            String token) {

        int maxRetries = 3;

        for (int i = 0; i < maxRetries; i++) {

            try {

                AuthApiResponse apiResponse =
                        authServiceClient.getUserById(
                                userId,
                                token
                        );

                if (apiResponse != null
                        && apiResponse.isSuccess()
                        && apiResponse.getData() != null) {

                    return apiResponse.getData();
                }

            } catch (Exception ex) {

                log.warning(
                        "getRequesterAuth attempt "
                                + (i + 1)
                                + " failed for userId="
                                + userId
                                + ": "
                                + ex.getMessage()
                );

                if (i < maxRetries - 1) {

                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }

        log.warning(
                "getRequesterAuth: all retries failed for userId="
                        + userId
        );

        return null;
    }

    private FriendRequestResponse toRequestResponse(
            FriendRequest fr,
            AuthUserResponse authUser) {

        FriendRequestResponse response =
                new FriendRequestResponse();

        response.setRequestId(fr.getId().toString());

        response.setRequesterId(
                fr.getRequesterId().toString()
        );

        response.setStatus(fr.getStatus().name());

        response.setCreatedAt(
                fr.getCreatedAt() != null
                        ? fr.getCreatedAt().toString()
                        : null
        );

        if (authUser != null) {

            response.setRequesterUsername(
                    authUser.getUsername()
            );

            response.setEmail(authUser.getEmail());

            response.setPhoneNumber(
                    authUser.getPhoneNumber()
            );

            response.setProfilePicture(
                    authUser.getProfilePicture()
            );

        } else {

            response.setRequesterUsername(
                    fr.getRequesterUsername()
            );

            response.setEmail(fr.getEmail());

            response.setPhoneNumber(
                    fr.getPhoneNumber()
            );
        }

        return response;
    }
}