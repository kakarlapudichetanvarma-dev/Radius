package com.chatservice.repository;

import com.chatservice.entity.CommunityInvite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityInviteRepository extends JpaRepository<CommunityInvite, UUID> {

    Optional<CommunityInvite> findByTokenAndIsActiveTrue(String token);

    List<CommunityInvite> findByCommunityIdAndIsActiveTrue(UUID communityId);

    void deleteByCommunityId(UUID communityId);
}