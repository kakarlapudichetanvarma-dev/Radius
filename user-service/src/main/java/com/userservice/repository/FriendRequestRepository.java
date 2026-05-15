package com.userservice.repository;

import com.userservice.entity.FriendRequest;
import com.userservice.entity.FriendRequest.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FriendRequestRepository
        extends JpaRepository<FriendRequest, UUID> {

    // FIX: only returns a request if it is PENDING or ACCEPTED.
    // REJECTED requests are ignored so either user can send a new
    // request after a rejection.
    @Query("""
            SELECT fr FROM FriendRequest fr
            WHERE (
                (fr.requesterId = :a AND fr.receiverId = :b)
             OR (fr.requesterId = :b AND fr.receiverId = :a)
            )
            AND fr.status IN (:activeStatuses)
            """)
    Optional<FriendRequest> findBetweenWithStatuses(
            @Param("a") UUID a,
            @Param("b") UUID b,
            @Param("activeStatuses") List<Status> activeStatuses
    );

    List<FriendRequest> findByReceiverIdAndStatus(
            UUID receiverId,
            Status status
    );

    List<FriendRequest> findByRequesterIdAndStatus(
            UUID requesterId,
            Status status
    );

    Optional<FriendRequest> findByIdAndReceiverIdAndStatus(
            UUID id,
            UUID receiverId,
            Status status
    );
}