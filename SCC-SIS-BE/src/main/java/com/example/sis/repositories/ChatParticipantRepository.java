package com.example.sis.repositories;

import com.example.sis.models.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByRoom_RoomId(Long roomId);

    Optional<ChatParticipant> findByRoom_RoomIdAndUser_UserId(Long roomId, Integer userId);

    boolean existsByRoom_RoomIdAndUser_UserId(Long roomId, Integer userId);
}
