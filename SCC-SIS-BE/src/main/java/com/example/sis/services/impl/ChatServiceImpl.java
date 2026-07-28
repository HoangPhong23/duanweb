package com.example.sis.services.impl;

import com.example.sis.dtos.chat.*;
import com.example.sis.enums.ChatRoomType;
import com.example.sis.enums.EnrollmentStatus;
import com.example.sis.enums.MessageType;
import com.example.sis.exceptions.BadRequestException;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.models.*;
import com.example.sis.repositories.*;
import com.example.sis.services.ChatService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
public class ChatServiceImpl implements ChatService {

    private final ChatRoomRepository roomRepo;
    private final ChatParticipantRepository participantRepo;
    private final UserChatMessageRepository messageRepo;
    private final UserRepository userRepo;
    private final ClassRepository classRepo;
    private final EnrollmentRepository enrollmentRepo;
    private final ClassTeacherRepository classTeacherRepo;
    private final SimpMessagingTemplate messagingTemplate;

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    public ChatServiceImpl(ChatRoomRepository roomRepo,
                           ChatParticipantRepository participantRepo,
                           UserChatMessageRepository messageRepo,
                           UserRepository userRepo,
                           ClassRepository classRepo,
                           EnrollmentRepository enrollmentRepo,
                           ClassTeacherRepository classTeacherRepo,
                           SimpMessagingTemplate messagingTemplate) {
        this.roomRepo = roomRepo;
        this.participantRepo = participantRepo;
        this.messageRepo = messageRepo;
        this.userRepo = userRepo;
        this.classRepo = classRepo;
        this.enrollmentRepo = enrollmentRepo;
        this.classTeacherRepo = classTeacherRepo;
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatRoomDTO> getUserChatRooms(Integer currentUserId) {
        List<ChatRoom> rooms = roomRepo.findRoomsByUserId(currentUserId);
        List<ChatRoomDTO> dtos = new ArrayList<>();

        for (ChatRoom room : rooms) {
            ChatRoomDTO dto = convertToRoomDTO(room, currentUserId);
            dtos.add(dto);
        }

        return dtos;
    }

    @Override
    public ChatRoomDTO getOrCreateDirectRoom(Integer currentUserId, Integer partnerUserId) {
        if (currentUserId.equals(partnerUserId)) {
            throw new BadRequestException("Không thể tạo cuộc trò chuyện với chính mình");
        }

        User partner = userRepo.findById(partnerUserId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy người dùng đối phương: " + partnerUserId));

        Optional<ChatRoom> existingRoom = roomRepo.findDirectRoomBetweenUsers(currentUserId, partnerUserId);
        ChatRoom room;

        if (existingRoom.isPresent()) {
            room = existingRoom.get();
        } else {
            User currentUser = userRepo.findById(currentUserId)
                    .orElseThrow(() -> new NotFoundException("User không tồn tại: " + currentUserId));

            room = new ChatRoom();
            room.setRoomType(ChatRoomType.DIRECT);
            room.setName(partner.getFullName());
            room = roomRepo.save(room);

            ChatParticipant p1 = new ChatParticipant();
            p1.setRoom(room);
            p1.setUser(currentUser);

            ChatParticipant p2 = new ChatParticipant();
            p2.setRoom(room);
            p2.setUser(partner);

            participantRepo.save(p1);
            participantRepo.save(p2);
        }

        return convertToRoomDTO(room, currentUserId);
    }

    @Override
    public ChatRoomDTO getOrCreateClassGroupRoom(Integer currentUserId, Integer classId) {
        ClassEntity classEntity = classRepo.findById(classId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy lớp học: " + classId));

        Optional<ChatRoom> existingRoom = roomRepo.findByRoomTypeAndClassEntity_ClassId(ChatRoomType.CLASS_GROUP, classId);
        ChatRoom room;

        if (existingRoom.isPresent()) {
            room = existingRoom.get();
        } else {
            room = new ChatRoom();
            room.setRoomType(ChatRoomType.CLASS_GROUP);
            room.setClassEntity(classEntity);
            room.setName("Nhóm lớp " + classEntity.getName());
            room = roomRepo.save(room);

            Set<Integer> userIdsToAdd = new HashSet<>();

            // 1. Add giảng viên từ class_teachers
            List<ClassTeacher> teachers = classTeacherRepo.findAllByClassId(classId);
            for (ClassTeacher ct : teachers) {
                if (ct.getTeacher() != null) {
                    userIdsToAdd.add(ct.getTeacher().getUserId());
                }
            }

            // 2. Add học viên từ enrollments
            List<Enrollment> enrollments = enrollmentRepo.findByClassEntity_ClassIdAndStatusAndRevokedAtIsNull(classId, EnrollmentStatus.ACTIVE);
            for (Enrollment e : enrollments) {
                if (e.getStudent() != null && e.getStudent().getUser() != null) {
                    userIdsToAdd.add(e.getStudent().getUser().getUserId());
                }
            }

            for (Integer uId : userIdsToAdd) {
                User u = userRepo.findById(uId).orElse(null);
                if (u != null) {
                    ChatParticipant p = new ChatParticipant();
                    p.setRoom(room);
                    p.setUser(u);
                    participantRepo.save(p);
                }
            }
        }

        // Đảm bảo user hiện tại có trong room
        if (!participantRepo.existsByRoom_RoomIdAndUser_UserId(room.getRoomId(), currentUserId)) {
            User currentUser = userRepo.findById(currentUserId).orElse(null);
            if (currentUser != null) {
                ChatParticipant p = new ChatParticipant();
                p.setRoom(room);
                p.setUser(currentUser);
                participantRepo.save(p);
            }
        }

        return convertToRoomDTO(room, currentUserId);
    }

    @Override
    public List<ChatMessageDTO> getRoomMessages(Long roomId, Integer currentUserId) {
        if (!participantRepo.existsByRoom_RoomIdAndUser_UserId(roomId, currentUserId)) {
            ChatRoom room = roomRepo.findById(roomId).orElse(null);
            User user = userRepo.findById(currentUserId).orElse(null);
            if (room != null && user != null) {
                ChatParticipant p = new ChatParticipant();
                p.setRoom(room);
                p.setUser(user);
                participantRepo.save(p);
            }
        }

        markRoomAsRead(roomId, currentUserId);

        List<ChatMessage> messages = messageRepo.findByRoom_RoomIdOrderByCreatedAtAsc(roomId);
        List<ChatMessageDTO> dtos = new ArrayList<>();

        for (ChatMessage msg : messages) {
            dtos.add(convertToMessageDTO(msg));
        }

        return dtos;
    }

    @Override
    public ChatMessageDTO saveAndSendMessage(Integer senderUserId, SendMessageDTO dto) {
        if (dto.getRoomId() == null) {
            throw new BadRequestException("Phòng chat không được để trống");
        }

        ChatRoom room = roomRepo.findById(dto.getRoomId())
                .orElseThrow(() -> new NotFoundException("Phòng chat không tồn tại: " + dto.getRoomId()));

        User sender = userRepo.findById(senderUserId)
                .orElseThrow(() -> new NotFoundException("User không tồn tại: " + senderUserId));

        if (!participantRepo.existsByRoom_RoomIdAndUser_UserId(dto.getRoomId(), senderUserId)) {
            ChatParticipant p = new ChatParticipant();
            p.setRoom(room);
            p.setUser(sender);
            participantRepo.save(p);
        }

        ChatMessage msg = new ChatMessage();
        msg.setRoom(room);
        msg.setSender(sender);
        msg.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : MessageType.TEXT);
        msg.setContent(dto.getContent());
        msg.setAttachmentUrl(dto.getAttachmentUrl());
        msg.setFileName(dto.getFileName());
        msg.setFileSize(dto.getFileSize());
        msg.setCreatedAt(LocalDateTime.now());

        msg = messageRepo.save(msg);

        roomRepo.updateRoomTimestamp(room.getRoomId(), LocalDateTime.now());

        markRoomAsRead(room.getRoomId(), senderUserId);

        ChatMessageDTO msgDTO = convertToMessageDTO(msg);

        messagingTemplate.convertAndSend("/topic/chat/" + room.getRoomId(), msgDTO);

        return msgDTO;
    }

    @Override
    public String uploadChatAttachment(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File rỗng");
        }

        try {
            File dir = new File(uploadDir + "/chat");
            if (!dir.exists()) {
                dir.mkdirs();
            }

            String ext = "";
            String originalName = file.getOriginalFilename();
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf("."));
            }

            String newFileName = UUID.randomUUID().toString() + ext;
            File dest = new File(dir, newFileName);
            file.transferTo(dest);

            return "/uploads/chat/" + newFileName;
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu file chat: " + e.getMessage(), e);
        }
    }

    @Override
    public void markRoomAsRead(Long roomId, Integer currentUserId) {
        Optional<ChatParticipant> pOpt = participantRepo.findByRoom_RoomIdAndUser_UserId(roomId, currentUserId);
        if (pOpt.isPresent()) {
            ChatParticipant p = pOpt.get();
            p.setLastReadAt(LocalDateTime.now());
            participantRepo.save(p);
        }
    }

    private ChatRoomDTO convertToRoomDTO(ChatRoom room, Integer currentUserId) {
        ChatRoomDTO dto = new ChatRoomDTO();
        dto.setRoomId(room.getRoomId());
        dto.setRoomType(room.getRoomType());
        if (room.getClassEntity() != null) {
            dto.setClassId(room.getClassEntity().getClassId());
        }

        if (room.getRoomType() == ChatRoomType.DIRECT) {
            List<ChatParticipant> participants = participantRepo.findByRoom_RoomId(room.getRoomId());
            for (ChatParticipant p : participants) {
                if (!p.getUser().getUserId().equals(currentUserId)) {
                    dto.setRoomName(p.getUser().getFullName());
                    dto.setPartnerUserId(p.getUser().getUserId());
                    break;
                }
            }
            if (dto.getRoomName() == null) {
                dto.setRoomName("Trò chuyện cá nhân");
            }
        } else {
            dto.setRoomName(room.getName() != null ? room.getName() : "Nhóm lớp");
        }

        ChatMessage latest = messageRepo.findLatestMessageByRoomId(room.getRoomId());
        if (latest != null) {
            if (latest.getMessageType() == MessageType.IMAGE) {
                dto.setLastMessage("[Hình ảnh]");
            } else if (latest.getMessageType() == MessageType.FILE) {
                dto.setLastMessage("[Tệp đính kèm: " + (latest.getFileName() != null ? latest.getFileName() : "File") + "]");
            } else {
                dto.setLastMessage(latest.getContent());
            }
            dto.setLastMessageTime(latest.getCreatedAt());
        }

        Optional<ChatParticipant> myParticipantOpt = participantRepo.findByRoom_RoomIdAndUser_UserId(room.getRoomId(), currentUserId);
        LocalDateTime lastRead = myParticipantOpt.map(ChatParticipant::getLastReadAt).orElse(null);
        long unread = messageRepo.countUnreadMessages(room.getRoomId(), currentUserId, lastRead);
        dto.setUnreadCount(unread);

        return dto;
    }

    private ChatMessageDTO convertToMessageDTO(ChatMessage msg) {
        ChatMessageDTO dto = new ChatMessageDTO();
        dto.setMessageId(msg.getMessageId());
        dto.setRoomId(msg.getRoom().getRoomId());
        dto.setSenderId(msg.getSender().getUserId());
        dto.setSenderName(msg.getSender().getFullName());
        dto.setMessageType(msg.getMessageType());
        dto.setContent(msg.getContent());
        dto.setAttachmentUrl(msg.getAttachmentUrl());
        dto.setFileName(msg.getFileName());
        dto.setFileSize(msg.getFileSize());
        dto.setCreatedAt(msg.getCreatedAt());
        return dto;
    }
}
