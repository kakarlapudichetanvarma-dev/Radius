package com.chatservice.repository;

import com.chatservice.entity.GroupEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GroupEventRepository extends JpaRepository<GroupEvent, UUID> {

    List<GroupEvent> findByGroupIdOrderByOccurredAtAsc(UUID groupId);

    // Added: filter events by type (e.g. MEMBER_ADDED, MEMBER_REMOVED, PROMOTED)
    List<GroupEvent> findByGroupIdAndEventTypeOrderByOccurredAtAsc(
            UUID groupId, String eventType);
}
