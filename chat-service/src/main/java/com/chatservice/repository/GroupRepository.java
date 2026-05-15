package com.chatservice.repository;

import com.chatservice.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GroupRepository extends JpaRepository<Group, UUID> {

    Optional<Group> findByChatId(UUID chatId);

    // Added: find all groups created by a user
    List<Group> findByCreatorId(UUID creatorId);
}
