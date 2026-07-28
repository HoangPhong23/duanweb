package com.example.sis.dtos.chat;

import com.example.sis.enums.ChatRoomType;
import java.time.LocalDateTime;

public class ChatRoomDTO {
    private Long roomId;
    private ChatRoomType roomType;
    private Integer classId;
    private String roomName;
    private String avatarUrl;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private long unreadCount;
    private Integer partnerUserId;

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

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getRoomName() {
        return roomName;
    }

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public LocalDateTime getLastMessageTime() {
        return lastMessageTime;
    }

    public void setLastMessageTime(LocalDateTime lastMessageTime) {
        this.lastMessageTime = lastMessageTime;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Integer getPartnerUserId() {
        return partnerUserId;
    }

    public void setPartnerUserId(Integer partnerUserId) {
        this.partnerUserId = partnerUserId;
    }
}
