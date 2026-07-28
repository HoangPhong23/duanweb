package com.example.sis.controllers;

import com.example.sis.dtos.chat.ChatMessageDTO;
import com.example.sis.dtos.chat.SendMessageDTO;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.ChatService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
public class ChatWebSocketController {

    private final ChatService chatService;
    private final UserRoleRepository userRoleRepository;

    public ChatWebSocketController(ChatService chatService, UserRoleRepository userRoleRepository) {
        this.chatService = chatService;
        this.userRoleRepository = userRoleRepository;
    }

    @MessageMapping("/chat.sendMessage")
    public ChatMessageDTO processMessage(SendMessageDTO dto, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal == null) {
            return null;
        }

        String keycloakUserId = principal.getName();
        Integer senderUserId = userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);

        if (senderUserId == null) {
            return null;
        }

        return chatService.saveAndSendMessage(senderUserId, dto);
    }
}
