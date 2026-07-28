package com.example.sis.repositories;

import com.example.sis.models.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    List<ChatMessage> findByRoom_RoomIdOrderByCreatedAtAsc(Long roomId);

    List<ChatMessage> findByRoom_RoomIdOrderByCreatedAtDesc(Long roomId, Pageable pageable);

    @Query("""
        SELECT COUNT(m) FROM UserChatMessage m
        WHERE m.room.roomId = :roomId
        AND (:lastReadAt IS NULL OR m.createdAt > :lastReadAt)
        AND m.sender.userId != :userId
    """)
    long countUnreadMessages(@Param("roomId") Long roomId,
                             @Param("userId") Integer userId,
                             @Param("lastReadAt") LocalDateTime lastReadAt);

    @Query("""
        SELECT m FROM UserChatMessage m
        WHERE m.room.roomId = :roomId
        ORDER BY m.createdAt DESC
        LIMIT 1
    """)
    ChatMessage findLatestMessageByRoomId(@Param("roomId") Long roomId);
}
