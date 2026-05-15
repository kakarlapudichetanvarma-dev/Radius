package com.chatservice.repository;

import com.chatservice.entity.ChatSearchIndex;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface ChatSearchIndexRepository extends JpaRepository<ChatSearchIndex, UUID> {

    // Basic text search in a single chat
    @Query("""
        SELECT s FROM ChatSearchIndex s
        WHERE s.chatId = :chatId
        AND (
            LOWER(s.content)  LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.fileName) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.url)   LIKE LOWER(CONCAT('%', :query, '%'))
        )
        ORDER BY s.sentAt DESC
        """)
    List<ChatSearchIndex> searchInChat(
            @Param("chatId") UUID chatId,
            @Param("query")  String query);

    // Search by sender ID in a chat
    @Query("""
        SELECT s FROM ChatSearchIndex s
        WHERE s.chatId = :chatId
        AND s.senderId = :senderId
        ORDER BY s.sentAt DESC
        """)
    List<ChatSearchIndex> findByChatIdAndSenderId(
            @Param("chatId")   UUID chatId,
            @Param("senderId") UUID senderId);

    // Search by sender username in a chat
    @Query("""
        SELECT s FROM ChatSearchIndex s
        WHERE s.chatId = :chatId
        AND LOWER(s.senderUsername) LIKE LOWER(CONCAT('%', :username, '%'))
        ORDER BY s.sentAt DESC
        """)
    List<ChatSearchIndex> findByChatIdAndSenderUsername(
            @Param("chatId")   UUID chatId,
            @Param("username") String username);

    // Search by date range in a chat
    @Query("""
        SELECT s FROM ChatSearchIndex s
        WHERE s.chatId = :chatId
        AND s.sentAt BETWEEN :from AND :to
        ORDER BY s.sentAt ASC
        """)
    List<ChatSearchIndex> findByChatIdAndDateRange(
            @Param("chatId") UUID chatId,
            @Param("from")   Instant from,
            @Param("to")     Instant to);

    // Search by mediaType in a chat
    @Query("""
        SELECT s FROM ChatSearchIndex s
        WHERE s.chatId = :chatId
        AND s.mediaType = :mediaType
        ORDER BY s.sentAt DESC
        """)
    List<ChatSearchIndex> findByChatIdAndMediaType(
            @Param("chatId")    UUID chatId,
            @Param("mediaType") String mediaType);

    // Search across multiple archived chats (req #25)
    @Query("""
        SELECT s FROM ChatSearchIndex s
        WHERE s.chatId IN :chatIds
        AND (
            LOWER(s.content)  LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.fileName) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.url)   LIKE LOWER(CONCAT('%', :query, '%'))
        )
        ORDER BY s.sentAt DESC
        """)
    List<ChatSearchIndex> searchInArchivedChats(
            @Param("chatIds") Collection<UUID> chatIds,
            @Param("query")   String query);

    // Full search with all filters combined
    @Query("""
        SELECT s FROM ChatSearchIndex s
        WHERE s.chatId = :chatId
        AND (:query IS NULL OR
            LOWER(s.content)  LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.fileName) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(s.url)   LIKE LOWER(CONCAT('%', :query, '%'))
        )
        AND (:senderId IS NULL OR s.senderId = :senderId)
        AND (:mediaType IS NULL OR s.mediaType = :mediaType)
        AND (:from IS NULL OR s.sentAt >= :from)
        AND (:to IS NULL OR s.sentAt <= :to)
        ORDER BY s.sentAt DESC
        """)
    List<ChatSearchIndex> searchWithFilters(
            @Param("chatId")    UUID chatId,
            @Param("query")     String query,
            @Param("senderId")  UUID senderId,
            @Param("mediaType") String mediaType,
            @Param("from")      Instant from,
            @Param("to")        Instant to);
}
