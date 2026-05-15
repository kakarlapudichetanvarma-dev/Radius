package com.chatservice.repository;

import com.chatservice.entity.MediaAttachment;
import com.chatservice.entity.MediaAttachment.MediaType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface MediaAttachmentRepository extends JpaRepository<MediaAttachment, UUID> {

    List<MediaAttachment> findByMessageId(UUID messageId);

    // FIX: removed JPQL "JOIN Message m ON ma.messageId = m.id" — JPA doesn't
    // support JOIN ON without a mapped relationship. MediaAttachment now has
    // chatId column directly, so we query it without any join.
    List<MediaAttachment> findByChatIdAndMediaTypeOrderByUploadedAtDesc(
            UUID chatId, MediaType mediaType);

    // Get all attachments for a chat regardless of type
    List<MediaAttachment> findByChatIdOrderByUploadedAtDesc(UUID chatId);

    // Get attachments for a list of message IDs (batch load — avoids N+1)
    @Query("SELECT ma FROM MediaAttachment ma WHERE ma.messageId IN :messageIds")
    List<MediaAttachment> findByMessageIds(@Param("messageIds") List<UUID> messageIds);

    // Search attachments by file name
    @Query("""
        SELECT ma FROM MediaAttachment ma
        WHERE ma.chatId = :chatId
        AND ma.mediaType = :mediaType
        AND LOWER(ma.fileName) LIKE LOWER(CONCAT('%', :query, '%'))
        ORDER BY ma.uploadedAt DESC
        """)
    List<MediaAttachment> searchByFileName(
            @Param("chatId") UUID chatId,
            @Param("mediaType") MediaType mediaType,
            @Param("query") String query);
}
