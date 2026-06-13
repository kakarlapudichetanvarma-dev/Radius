package com.chatservice.repository;

import com.chatservice.entity.CommunityMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityMemberRepository extends JpaRepository<CommunityMember, UUID> {

    List<CommunityMember> findByCommunityId(UUID communityId);

    Optional<CommunityMember> findByCommunityIdAndUserId(UUID communityId, UUID userId);

    boolean existsByCommunityIdAndUserId(UUID communityId, UUID userId);

    List<CommunityMember> findByUserId(UUID userId);

    void deleteByCommunityIdAndUserId(UUID communityId, UUID userId);
}