package com.chatservice.ai.repository;

import com.chatservice.ai.entity.AiMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, UUID> {

    List<AiMessage> findByConversationIdOrderBySentAtAsc(UUID conversationId);

    List<AiMessage> findByConversationIdOrderBySentAtDesc(UUID conversationId, Pageable pageable);
}