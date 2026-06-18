package com.chatservice.ai.repository;

import com.chatservice.ai.entity.AiConversation;
import com.chatservice.ai.entity.AiConversation.ConversationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, UUID> {

    List<AiConversation> findByUserIdAndConversationType(UUID userId, ConversationType type);

    Optional<AiConversation> findFirstByUserIdAndConversationTypeOrderByUpdatedAtDesc(
            UUID userId, ConversationType type);
}