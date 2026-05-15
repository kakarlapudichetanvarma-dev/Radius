package com.chatservice.repository;

import com.chatservice.entity.MessageEdit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface MessageEditRepository extends JpaRepository<MessageEdit, UUID> {
    List<MessageEdit> findByMessageIdOrderByEditedAtAsc(UUID messageId);
}