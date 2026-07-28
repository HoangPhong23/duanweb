package com.example.sis.models;

import com.example.sis.enums.ChatRoomType;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "chat_rooms")
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "room_id")
    private Long roomId;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 20)
    private ChatRoomType roomType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id")
    private ClassEntity classEntity;

    @Column(name = "name")
    private String name;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public ChatRoomType getRoomType() {
        return roomType;
    }

    public void setRoomType(ChatRoomType roomType) {
        this.roomType = roomType;
    }

    public ClassEntity getClassEntity() {
        return classEntity;
    }

    public void setClassEntity(ClassEntity classEntity) {
        this.classEntity = classEntity;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
