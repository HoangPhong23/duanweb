package com.example.sis.controllers;

import com.example.sis.dtos.chat.*;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class UserChatController {

    private final ChatService chatService;
    private final UserRoleRepository userRoleRepository;

    public UserChatController(ChatService chatService, UserRoleRepository userRoleRepository) {
        this.chatService = chatService;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * GET /api/chat/rooms
     * Lấy danh sách các cuộc trò chuyện của user hiện tại
     */
    @GetMapping("/rooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatRoomDTO>> getUserRooms(Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<ChatRoomDTO> rooms = chatService.getUserChatRooms(currentUserId);
        return ResponseEntity.ok(rooms);
    }

    /**
     * POST /api/chat/direct
     * Tạo hoặc tìm phòng chat 1-1 với partnerUserId
     */
    @PostMapping("/direct")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomDTO> getOrCreateDirectRoom(
            @RequestBody CreateDirectChatDTO request,
            Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ChatRoomDTO room = chatService.getOrCreateDirectRoom(currentUserId, request.getPartnerUserId());
        return ResponseEntity.ok(room);
    }

    /**
     * POST /api/chat/class-group/{classId}
     * Mở hoặc tạo nhóm chat lớp
     */
    @PostMapping("/class-group/{classId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatRoomDTO> getOrCreateClassGroupRoom(
            @PathVariable Integer classId,
            Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ChatRoomDTO room = chatService.getOrCreateClassGroupRoom(currentUserId, classId);
        return ResponseEntity.ok(room);
    }

    /**
     * GET /api/chat/rooms/{roomId}/messages
     * Lấy lịch sử tin nhắn phòng chat
     */
    @GetMapping("/rooms/{roomId}/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ChatMessageDTO>> getRoomMessages(
            @PathVariable Long roomId,
            Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        List<ChatMessageDTO> messages = chatService.getRoomMessages(roomId, currentUserId);
        return ResponseEntity.ok(messages);
    }

    /**
     * POST /api/chat/messages
     * Gửi tin nhắn qua REST API
     */
    @PostMapping("/messages")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ChatMessageDTO> sendMessage(
            @RequestBody SendMessageDTO request,
            Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        ChatMessageDTO msg = chatService.saveAndSendMessage(currentUserId, request);
        return ResponseEntity.ok(msg);
    }

    /**
     * POST /api/chat/upload
     * Upload file/ảnh đính kèm tin nhắn
     */
    @PostMapping("/upload")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> uploadAttachment(@RequestParam("file") MultipartFile file) {
        String url = chatService.uploadChatAttachment(file);
        Map<String, String> res = new HashMap<>();
        res.put("url", url);
        res.put("fileName", file.getOriginalFilename());
        res.put("fileSize", String.valueOf(file.getSize()));
        return ResponseEntity.ok(res);
    }

    /**
     * PUT /api/chat/rooms/{roomId}/read
     * Đánh dấu đã đọc phòng chat
     */
    @PutMapping("/rooms/{roomId}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Void> markAsRead(
            @PathVariable Long roomId,
            Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        chatService.markRoomAsRead(roomId, currentUserId);
        return ResponseEntity.ok().build();
    }

    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject();
            return userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
        }
        return null;
    }
}
