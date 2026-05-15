package com.chatservice.repository;

import com.chatservice.entity.ChatSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ChatSettingsRepository extends JpaRepository<ChatSettings, UUID> {
    Optional<ChatSettings> findByChatIdAndUserId(UUID chatId, UUID userId);
}
