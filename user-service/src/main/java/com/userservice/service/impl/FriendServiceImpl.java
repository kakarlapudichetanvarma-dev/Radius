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

        // Step 1: Fetch receiver
        AuthApiResponse receiverApiResponse =
                authServiceClient.getUserByPhone(targetPhoneNumber, token);

        if (receiverApiResponse == null
                || !receiverApiResponse.isSuccess()
                || receiverApiResponse.getData() == null) {
            throw new RuntimeException(
                    "Could not find user with phone number: " + targetPhoneNumber);
        }

        AuthUserResponse receiverAuth = receiverApiResponse.getData();
        UUID receiverId = UUID.fromString(receiverAuth.getId());

        if (receiverId.equals(requesterId)) {
            throw new SelfFriendRequestException(
                    "You cannot send a friend request to yourself.");
        }

        // Step 2: Already friends?
        if (friendRepository.existsByUserIdAndFriendId(requesterId, receiverId)) {
            throw new AlreadyFriendsException("You are already friends with this user.");
        }

        // Step 3: Check existing request
        Optional<FriendRequest> existingOpt =
                friendRequestRepository.findBetweenWithStatuses(
                        requesterId, receiverId,
                        List.of(Status.PENDING, Status.ACCEPTED, Status.REJECTED));

        if (existingOpt.isPresent()) {
            FriendRequest existing = existingOpt.get();

            if (existing.getStatus() == Status.PENDING) {
                throw new FriendRequestAlreadyExistsException(
                        "A friend request is already pending.");
            }
            if (existing.getStatus() == Status.ACCEPTED) {
                throw new AlreadyFriendsException("You are already friends.");
            }
            if (existing.getStatus() == Status.REJECTED) {
                if (existing.getRequesterId().equals(requesterId)) {
                    AuthUserResponse requesterAuth = getRequesterAuth(requesterId, token);
                    existing.setStatus(Status.PENDING);
                    existing.setRequesterUsername(requesterAuth.getUsername());
                    existing.setEmail(requesterAuth.getEmail());
                    existing.setPhoneNumber(requesterAuth.getPhoneNumber());
                    FriendRequest updated = friendRequestRepository.save(existing);
                    return toRequestResponse(updated, requesterAuth);
                }
                friendRequestRepository.delete(existing);
            }
        }

        // Step 4: Create new request
        AuthUserResponse requesterAuth = getRequesterAuth(requesterId, token);

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

        FriendRequest fr = friendRequestRepository
                .findByIdAndReceiverIdAndStatus(requestId, receiverId, Status.PENDING)
                .orElseThrow(() -> new FriendRequestNotFoundException(
                        "No pending friend request found."));

        if (accept) {
            fr.setStatus(Status.ACCEPTED);

            // Fetch BOTH users
            AuthUserResponse requesterAuth = getRequesterAuth(fr.getRequesterId(), token);
            AuthUserResponse receiverAuth  = getRequesterAuth(fr.getReceiverId(), token);

            // ── Debug logs so you can see exactly what comes back ──────────
            log.info("=== ACCEPT FRIEND REQUEST ===");
            log.info("requesterAuth: " + (requesterAuth == null ? "NULL" :
                    requesterAuth.getUsername() + " / " + requesterAuth.getEmail()
                            + " / " + requesterAuth.getPhoneNumber()));
            log.info("receiverAuth: " + (receiverAuth == null ? "NULL" :
                    receiverAuth.getUsername() + " / " + receiverAuth.getEmail()
                            + " / " + receiverAuth.getPhoneNumber()));

            LocalDateTime now = LocalDateTime.now();

            // friend1 row: owned by requester, stores RECEIVER's profile
            Friend friend1 = new Friend();
            friend1.setUserId(fr.getRequesterId());
            friend1.setFriendId(fr.getReceiverId());
            friend1.setSince(now);
            if (receiverAuth != null) {
                friend1.setUsername(receiverAuth.getUsername());
                friend1.setEmail(receiverAuth.getEmail());
                friend1.setPhoneNumber(receiverAuth.getPhoneNumber());
                friend1.setProfilePicture(receiverAuth.getProfilePicture());
            }
            friendRepository.save(friend1);
            log.info("Saved friend1 → userId=" + fr.getRequesterId()
                    + " friendId=" + fr.getReceiverId()
                    + " username=" + friend1.getUsername());

            // friend2 row: owned by receiver, stores REQUESTER's profile
            Friend friend2 = new Friend();
            friend2.setUserId(fr.getReceiverId());
            friend2.setFriendId(fr.getRequesterId());
            friend2.setSince(now);
            if (requesterAuth != null) {
                friend2.setUsername(requesterAuth.getUsername());
                friend2.setEmail(requesterAuth.getEmail());
                friend2.setPhoneNumber(requesterAuth.getPhoneNumber());
                friend2.setProfilePicture(requesterAuth.getProfilePicture());
            }
            friendRepository.save(friend2);
            log.info("Saved friend2 → userId=" + fr.getReceiverId()
                    + " friendId=" + fr.getRequesterId()
                    + " username=" + friend2.getUsername());

            fr = friendRequestRepository.save(fr);
            return toRequestResponse(fr, requesterAuth);

        } else {
            fr.setStatus(Status.REJECTED);
            fr = friendRequestRepository.save(fr);
            AuthUserResponse requesterAuth = getRequesterAuth(fr.getRequesterId(), token);
            return toRequestResponse(fr, requesterAuth);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List friends  ← phoneNumber fix is here
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

                    // ── Use stored DB values as fallback (works even if auth is down)
                    response.setUsername(f.getUsername());
                    response.setEmail(f.getEmail());           // ← was missing
                    response.setPhoneNumber(f.getPhoneNumber()); // ← was missing
                    response.setProfilePicture(f.getProfilePicture());

                    // ── Try live refresh from auth service
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
                        log.warning("Failed to fetch friend profile for "
                                + f.getFriendId() + ": " + ex.getMessage());
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // List pending requests
    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public List<FriendRequestResponse> listPendingRequests(UUID userId, String token) {
        return friendRequestRepository
                .findByReceiverIdAndStatus(userId, Status.PENDING)
                .stream()
                .map(fr -> {
                    FriendRequestResponse response = new FriendRequestResponse();
                    response.setRequestId(fr.getId().toString());
                    response.setRequesterId(fr.getRequesterId().toString());
                    response.setStatus(fr.getStatus().name());
                    response.setCreatedAt(
                            fr.getCreatedAt() != null ? fr.getCreatedAt().toString() : null);

                    // Stored snapshot as fallback
                    response.setRequesterUsername(fr.getRequesterUsername());
                    response.setEmail(fr.getEmail());
                    response.setPhoneNumber(fr.getPhoneNumber());

                    try {
                        AuthApiResponse apiResponse =
                                authServiceClient.getUserById(fr.getRequesterId(), token);
                        if (apiResponse != null
                                && apiResponse.isSuccess()
                                && apiResponse.getData() != null) {
                            AuthUserResponse authUser = apiResponse.getData();
                            response.setRequesterUsername(authUser.getUsername());
                            response.setEmail(authUser.getEmail());
                            response.setPhoneNumber(authUser.getPhoneNumber());
                            response.setProfilePicture(authUser.getProfilePicture());
                        }
                    } catch (Exception ex) {
                        log.warning("Failed to fetch requester profile: " + ex.getMessage());
                    }
                    return response;
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────
    private AuthUserResponse getRequesterAuth(UUID userId, String token) {
        try {
            AuthApiResponse apiResponse = authServiceClient.getUserById(userId, token);
            if (apiResponse != null
                    && apiResponse.isSuccess()
                    && apiResponse.getData() != null) {
                return apiResponse.getData();
            }
            log.warning("getRequesterAuth: null/unsuccessful response for userId=" + userId);
        } catch (Exception ex) {
            log.warning("getRequesterAuth failed for userId=" + userId
                    + ": " + ex.getMessage());
        }
        return null;
    }

    private FriendRequestResponse toRequestResponse(
            FriendRequest fr, AuthUserResponse authUser) {

        FriendRequestResponse response = new FriendRequestResponse();
        response.setRequestId(fr.getId().toString());
        response.setRequesterId(fr.getRequesterId().toString());
        response.setStatus(fr.getStatus().name());
        response.setCreatedAt(
                fr.getCreatedAt() != null ? fr.getCreatedAt().toString() : null);

        if (authUser != null) {
            response.setRequesterUsername(authUser.getUsername());
            response.setEmail(authUser.getEmail());
            response.setPhoneNumber(authUser.getPhoneNumber());
            response.setProfilePicture(authUser.getProfilePicture());
        } else {
            response.setRequesterUsername(fr.getRequesterUsername());
            response.setEmail(fr.getEmail());
            response.setPhoneNumber(fr.getPhoneNumber());
        }
        return response;
    }
}