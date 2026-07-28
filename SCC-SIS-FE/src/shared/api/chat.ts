import http from './http';

export type ChatRoomType = 'DIRECT' | 'CLASS_GROUP';
export type MessageType = 'TEXT' | 'IMAGE' | 'FILE';

export interface ChatRoom {
    roomId: number;
    roomType: ChatRoomType;
    classId?: number;
    roomName: string;
    avatarUrl?: string;
    lastMessage?: string;
    lastMessageTime?: string;
    unreadCount: number;
    partnerUserId?: number;
}

export interface ChatMessage {
    messageId: number;
    roomId: number;
    senderId: number;
    senderName: string;
    senderAvatar?: string;
    messageType: MessageType;
    content?: string;
    attachmentUrl?: string;
    fileName?: string;
    fileSize?: number;
    createdAt: string;
}

export interface SendMessagePayload {
    roomId: number;
    messageType?: MessageType;
    content?: string;
    attachmentUrl?: string;
    fileName?: string;
    fileSize?: number;
}

// 1. Lấy danh sách phòng chat
export const getUserChatRooms = () =>
    http.get<ChatRoom[]>('/api/chat/rooms');

// 2. Tạo/Mở phòng chat 1-1 với user khác
export const getOrCreateDirectRoom = (partnerUserId: number) =>
    http.post<ChatRoom>('/api/chat/direct', { partnerUserId });

// 3. Mở/Tạo nhóm chat lớp
export const getOrCreateClassGroupRoom = (classId: number) =>
    http.post<ChatRoom>(`/api/chat/class-group/${classId}`);

// 4. Lấy danh sách tin nhắn của phòng
export const getRoomMessages = (roomId: number) =>
    http.get<ChatMessage[]>(`/api/chat/rooms/${roomId}/messages`);

// 5. Gửi tin nhắn qua REST API
export const sendMessageApi = (data: SendMessagePayload) =>
    http.post<ChatMessage>('/api/chat/messages', data);

// 6. Upload file/ảnh đính kèm
export const uploadChatAttachment = (file: File) => {
    const formData = new FormData();
    formData.append('file', file);
    return http.post<{ url: string; fileName: string; fileSize: string }>('/api/chat/upload', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
    });
};

// 7. Đánh dấu phòng chat đã đọc
export const markRoomAsRead = (roomId: number) =>
    http.put(`/api/chat/rooms/${roomId}/read`);
