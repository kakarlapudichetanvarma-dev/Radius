package com.chatservice.repository;

import com.chatservice.entity.ArchivedChat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ArchivedChatRepository extends JpaRepository<ArchivedChat, UUID> {

    List<ArchivedChat> findByUserId(UUID userId);

    boolean existsByChatIdAndUserId(UUID chatId, UUID userId);

    Optional<ArchivedChat> findByChatIdAndUserId(UUID chatId, UUID userId);

    // Added: check which of a set of chatIds are archived for this user
    // Used by search-in-archived-chats to get the archived chat ID list
    List<ArchivedChat> findByUserIdAndChatIdIn(UUID userId, Collection<UUID> chatIds);
}
