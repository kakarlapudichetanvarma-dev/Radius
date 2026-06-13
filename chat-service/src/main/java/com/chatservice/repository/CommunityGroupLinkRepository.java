package com.chatservice.repository;

import com.chatservice.entity.CommunityGroupLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CommunityGroupLinkRepository extends JpaRepository<CommunityGroupLink, UUID> {

    List<CommunityGroupLink> findByCommunityIdAndIsVisibleTrue(UUID communityId);

    Optional<CommunityGroupLink> findByCommunityIdAndGroupId(UUID communityId, UUID groupId);

    boolean existsByCommunityIdAndGroupId(UUID communityId, UUID groupId);

    Optional<CommunityGroupLink> findByGroupId(UUID groupId);

    void deleteByCommunityIdAndGroupId(UUID communityId, UUID groupId);
}