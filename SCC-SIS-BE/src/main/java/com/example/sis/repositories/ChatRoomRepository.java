package com.example.sis.repositories;

import com.example.sis.enums.ChatRoomType;
import com.example.sis.models.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    Optional<ChatRoom> findByRoomTypeAndClassEntity_ClassId(ChatRoomType roomType, Integer classId);

    @Query("""
        SELECT r FROM ChatRoom r
        JOIN ChatParticipant p ON p.room.roomId = r.roomId
        WHERE p.user.userId = :userId
        ORDER BY r.updatedAt DESC
    """)
    List<ChatRoom> findRoomsByUserId(@Param("userId") Integer userId);

    @Query("""
        SELECT r FROM ChatRoom r
        JOIN ChatParticipant p1 ON p1.room.roomId = r.roomId AND p1.user.userId = :user1Id
        JOIN ChatParticipant p2 ON p2.room.roomId = r.roomId AND p2.user.userId = :user2Id
        WHERE r.roomType = 'DIRECT'
    """)
    Optional<ChatRoom> findDirectRoomBetweenUsers(@Param("user1Id") Integer user1Id, @Param("user2Id") Integer user2Id);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE ChatRoom r SET r.updatedAt = :now WHERE r.roomId = :roomId")
    void updateRoomTimestamp(@Param("roomId") Long roomId, @Param("now") java.time.LocalDateTime now);
}
