package com.chatservice.repository;

import com.chatservice.entity.MessageStar;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Repository
public interface MessageStarRepository extends JpaRepository<MessageStar, UUID> {

    boolean existsByMessageIdAndUserId(UUID messageId, UUID userId);

    Optional<MessageStar> findByMessageIdAndUserId(UUID messageId, UUID userId);

    @Modifying
    void deleteByMessageIdAndUserId(UUID messageId, UUID userId);

    // All starred messages for a user, most recently starred first
    List<MessageStar> findByUserIdOrderByStarredAtDesc(UUID userId);

    // Used by getChatMessages to know which of the loaded messages are
    // starred by the requesting user, in one query.
    @Query("""
        SELECT s.messageId FROM MessageStar s
        WHERE s.userId = :userId AND s.messageId IN :messageIds
        """)
    Set<UUID> findStarredMessageIdsByUserId(
            @Param("userId") UUID userId,
            @Param("messageIds") List<UUID> messageIds);

    // Cleanup helper — mirrors deleteByChatId on MessageRepository
    @Modifying
    void deleteByChatId(UUID chatId);
}