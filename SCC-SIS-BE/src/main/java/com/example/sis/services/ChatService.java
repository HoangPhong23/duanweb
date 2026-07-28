package com.example.sis.services;

import com.example.sis.dtos.chat.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ChatService {

    List<ChatRoomDTO> getUserChatRooms(Integer currentUserId);

    ChatRoomDTO getOrCreateDirectRoom(Integer currentUserId, Integer partnerUserId);

    ChatRoomDTO getOrCreateClassGroupRoom(Integer currentUserId, Integer classId);

    List<ChatMessageDTO> getRoomMessages(Long roomId, Integer currentUserId);

    ChatMessageDTO saveAndSendMessage(Integer senderUserId, SendMessageDTO dto);

    String uploadChatAttachment(MultipartFile file);

    void markRoomAsRead(Long roomId, Integer currentUserId);
}
