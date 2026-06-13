package com.chatservice.repository;

import com.chatservice.entity.Community;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CommunityRepository extends JpaRepository<Community, UUID> {

    List<Community> findByCreatedByAndIsActiveTrue(UUID createdBy);

    List<Community> findByIsActiveTrue();
}