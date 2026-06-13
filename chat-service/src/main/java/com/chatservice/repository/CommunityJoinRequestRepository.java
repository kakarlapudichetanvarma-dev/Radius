package com.chatservice.repository;

import com.chatservice.entity.CommunityJoinRequest;
import com.chatservice.entity.CommunityJoinRequest.Status;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityJoinRequestRepository extends JpaRepository<CommunityJoinRequest, UUID> {

    List<CommunityJoinRequest> findByCommunityIdAndStatus(UUID communityId, Status status);

    Optional<CommunityJoinRequest> findByCommunityIdAndUserId(UUID communityId, UUID userId);

    boolean existsByCommunityIdAndUserIdAndStatus(UUID communityId, UUID userId, Status status);

    void deleteByCommunityId(UUID communityId);
}