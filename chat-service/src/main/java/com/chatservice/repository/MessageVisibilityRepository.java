package com.chatservice.repository;

import com.chatservice.entity.MessageVisibility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface MessageVisibilityRepository extends JpaRepository<MessageVisibility, UUID> {

    boolean existsByMessageIdAndUserId(UUID messageId, UUID userId);

    Optional<MessageVisibility> findByMessageIdAndUserId(UUID messageId, UUID userId);

    // Added: get all hidden message IDs for a user — used for bulk filtering
    // instead of N+1 per-message DB calls
    @Query("SELECT mv.messageId FROM MessageVisibility mv WHERE mv.userId = :userId")
    Set<UUID> findHiddenMessageIdsByUserId(@Param("userId") UUID userId);

    // Added: bulk check — which of these messageIds are hidden for this user
    @Query("""
        SELECT mv.messageId FROM MessageVisibility mv
        WHERE mv.userId = :userId
        AND mv.messageId IN :messageIds
        """)
    Set<UUID> findHiddenMessageIdsForUser(
            @Param("userId")     UUID userId,
            @Param("messageIds") Collection<UUID> messageIds);

    List<MessageVisibility> findByUserId(UUID userId);
}
