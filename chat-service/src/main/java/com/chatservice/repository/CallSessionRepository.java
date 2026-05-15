package com.chatservice.repository;

import com.chatservice.entity.CallSession;
import com.chatservice.entity.CallSession.CallStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CallSessionRepository extends JpaRepository<CallSession, UUID> {

    // Get call history for a user (as caller or callee)
    @Query("""
        SELECT c FROM CallSession c
        WHERE c.callerId = :userId OR c.calleeId = :userId
        ORDER BY c.createdAt DESC
        """)
    List<CallSession> findCallHistoryByUserId(@Param("userId") UUID userId);

    // Get active call in a chat — uses List param to avoid string literal enum issue
    @Query("""
        SELECT c FROM CallSession c
        WHERE c.chatId = :chatId
        AND c.callStatus IN :statuses
        ORDER BY c.createdAt DESC
        """)
    Optional<CallSession> findActiveCallInChat(
            @Param("chatId")   UUID chatId,
            @Param("statuses") List<CallStatus> statuses);

    // Get all calls in a chat ordered by created time
    List<CallSession> findByChatIdOrderByCreatedAtDesc(UUID chatId);
}
