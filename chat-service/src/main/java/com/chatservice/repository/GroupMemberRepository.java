package com.chatservice.repository;

import com.chatservice.entity.GroupMember;
import com.chatservice.entity.GroupMember.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, UUID> {

    // Active members only (not left)
    List<GroupMember> findByGroupIdAndLeftAtIsNull(UUID groupId);

    Optional<GroupMember> findByGroupIdAndUserId(UUID groupId, UUID userId);

    boolean existsByGroupIdAndUserIdAndRole(UUID groupId, UUID userId, Role role);

    boolean existsByGroupIdAndUserId(UUID groupId, UUID userId);

    // All groups a user is a member of (active)
    List<GroupMember> findByUserIdAndLeftAtIsNull(UUID userId);
List<GroupMember> findByGroupId(UUID groupId);
    // All members including those who left (for audit)
    List<GroupMember> findByUserId(UUID userId);
}
