package com.chatservice.repository;

import com.chatservice.entity.GroupMember;
import com.chatservice.entity.Message;
import com.chatservice.entity.Message.MessageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageRepository extends JpaRepository<Message, UUID> {

    // FIX: filters out messages hidden for this user via MessageVisibility
    // and also filters out "delete for everyone" (isDeleted = true)
    @Query("""
        SELECT m FROM Message m
        WHERE m.chatId = :chatId
        AND m.isDeleted = false
        AND m.id NOT IN (
            SELECT mv.messageId FROM MessageVisibility mv
            WHERE mv.userId = :userId
        )
        ORDER BY m.sentAt ASC
        """)
    List<Message> findByChatIdOrderedForUser(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId);

    // Get recent N messages for cache population (no user filter — cache is shared)
    @Query(value = """
        SELECT * FROM messages
        WHERE chat_id = :chatId AND is_deleted = false
        ORDER BY sent_at DESC
        LIMIT :limit
        """, nativeQuery = true)
    List<Message> findRecentMessages(
            @Param("chatId") UUID chatId,
            @Param("limit") int limit);

    // Unread messages in a chat (not sent by this user)
    @Query("""
        SELECT m FROM Message m
        WHERE m.chatId = :chatId
        AND m.status = :status
        AND m.senderId != :userId
        AND m.isDeleted = false
        """)
    List<Message> findUnreadMessages(
            @Param("chatId") UUID chatId,
            @Param("status") MessageStatus status,
            @Param("userId") UUID userId);

    // Search by text content, filtered per user visibility
    @Query("""
        SELECT m FROM Message m
        WHERE m.chatId = :chatId
        AND m.isDeleted = false
        AND m.id NOT IN (
            SELECT mv.messageId FROM MessageVisibility mv WHERE mv.userId = :userId
        )
        AND LOWER(m.content) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY m.sentAt DESC
        """)
    List<Message> searchByContent(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId,
            @Param("query") String query);

    // Search by sender
    @Query("""
        SELECT m FROM Message m
        WHERE m.chatId = :chatId
        AND m.senderId = :senderId
        AND m.isDeleted = false
        AND m.id NOT IN (
            SELECT mv.messageId FROM MessageVisibility mv WHERE mv.userId = :userId
        )
        ORDER BY m.sentAt DESC
        """)
    List<Message> findBySender(
            @Param("chatId") UUID chatId,
            @Param("senderId") UUID senderId,
            @Param("userId") UUID userId);

    // Search by date range
    @Query("""
        SELECT m FROM Message m
        WHERE m.chatId = :chatId
        AND m.sentAt BETWEEN :from AND :to
        AND m.isDeleted = false
        AND m.id NOT IN (
            SELECT mv.messageId FROM MessageVisibility mv WHERE mv.userId = :userId
        )
        ORDER BY m.sentAt ASC
        """)
    List<Message> findByDateRange(
            @Param("chatId") UUID chatId,
            @Param("userId") UUID userId,
            @Param("from") Instant from,
            @Param("to") Instant to);

    // Load messages by IDs (used after bulk visibility filter)
    @Query("SELECT m FROM Message m WHERE m.id IN :ids ORDER BY m.sentAt ASC")
    List<Message> findByIds(@Param("ids") List<UUID> ids);
    @Modifying
@Query("DELETE FROM Message m WHERE m.chatId = :chatId")
void deleteByChatId(@Param("chatId") UUID chatId);

    List<Message> findByChatIdAndSenderId(UUID chatId, UUID senderId);
}
