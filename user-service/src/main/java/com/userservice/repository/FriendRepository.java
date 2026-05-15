package com.userservice.repository;

import com.userservice.entity.Friend;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FriendRepository
        extends JpaRepository<Friend, UUID> {

    List<Friend> findAllByUserId(
            UUID userId
    );

    boolean existsByUserIdAndFriendId(
            UUID userId,
            UUID friendId
    );
}