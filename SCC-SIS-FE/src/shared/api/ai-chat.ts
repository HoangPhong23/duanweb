import http from './http';

export interface ChatSessionDTO {
    sessionId: number;
    title: string;
    context?: string;
    createdAt: string;
    updatedAt: string;
    messageCount: number;
}

export interface ChatMessageResponse {
    messageId?: number;
    sessionId: number;
    role?: string;
    message: string;
    sources?: any[];
    completionMs?: number;
    timestamp: string;
}

export interface ChatSessionDetailsResponse {
    session: ChatSessionDTO;
    messages: ChatMessageResponse[];
}

export type AIChatMessage = {
    role: 'user' | 'assistant';
    content: string;
    timestamp: string;
};

export const createChatSession = (title: string = 'New Chat') =>
    http.post<ChatSessionDTO>('/api/chat/sessions', { title }).then((res) => res.data);

export const getUserChatSessions = () =>
    http.get<ChatSessionDTO[]>('/api/chat/sessions').then((res) => res.data);

export const getChatSessions = getUserChatSessions;

export const getChatSessionDetails = (sessionId: number) =>
    http.get<ChatSessionDetailsResponse>(`/api/chat/sessions/${sessionId}`).then((res) => res.data);

export const sendChatMessage = (sessionId: number, message: string, classId?: number, moduleId?: number, lessonId?: number) =>
    http.post<ChatMessageResponse>(`/api/chat/sessions/${sessionId}/messages`, {
        sessionId,
        message,
        classId,
        moduleId,
        lessonId
    }).then((res) => res.data);

export const deleteChatSession = (sessionId: number) =>
    http.delete(`/api/chat/sessions/${sessionId}`).then((res) => res.data);
