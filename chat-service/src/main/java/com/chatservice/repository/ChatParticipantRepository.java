package com.chatservice.repository;

import com.chatservice.entity.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, UUID> {

    List<ChatParticipant> findByChatId(UUID chatId);

    List<ChatParticipant> findByUserId(UUID userId);

    Optional<ChatParticipant> findByChatIdAndUserId(UUID chatId, UUID userId);

    boolean existsByChatIdAndUserId(UUID chatId, UUID userId);

    // FIX: returns List — multiple chats can have same username as participant
    List<ChatParticipant> findByUsername(String username);

    // Active participants only (not left)
    List<ChatParticipant> findByChatIdAndLeftAtIsNull(UUID chatId);

    // FIX: added Chat.type = 'PRIVATE' filter so group chats shared by both
    // users are excluded. Also filters leftAt IS NULL on both sides.
    @Query("""
        SELECT cp1.chatId FROM ChatParticipant cp1
        JOIN ChatParticipant cp2 ON cp1.chatId = cp2.chatId
        JOIN Chat c ON c.id = cp1.chatId
        WHERE cp1.userId = :userA
        AND cp2.userId = :userB
        AND c.type = 'PRIVATE'
        AND cp1.leftAt IS NULL
        AND cp2.leftAt IS NULL
        """)
    List<UUID> findPrivateChatBetween(
            @Param("userA") UUID userA,
            @Param("userB") UUID userB);

    // Check if user is still active in chat (not left)
    @Query("""
        SELECT CASE WHEN COUNT(cp) > 0 THEN true ELSE false END
        FROM ChatParticipant cp
        WHERE cp.chatId = :chatId
        AND cp.userId = :userId
        AND cp.leftAt IS NULL
        """)
    boolean isActiveParticipant(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId);

    // Get all chat IDs for a user (active only)
    @Query("""
        SELECT cp.chatId FROM ChatParticipant cp
        WHERE cp.userId = :userId
        AND cp.leftAt IS NULL
        """)
    List<UUID> findActiveChatIdsByUserId(@Param("userId") UUID userId);
}
