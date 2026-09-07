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

export const sendChatMessageStream = async (
    sessionId: number,
    message: string,
    onChunk: (text: string) => void,
    classId?: number,
    moduleId?: number,
    lessonId?: number
) => {
    // Get token directly from localStorage since we are using raw fetch
    const token = localStorage.getItem('access_token');
    
    // Determine base URL (handle both dev proxy and prod)
    const baseUrl = import.meta.env.VITE_API_URL || '';
    
    const response = await fetch(`${baseUrl}/api/chat/sessions/${sessionId}/messages/stream`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {})
        },
        body: JSON.stringify({
            sessionId,
            message,
            classId,
            moduleId,
            lessonId
        })
    });

    if (!response.ok) {
        throw new Error('Failed to send message');
    }

    const reader = response.body?.getReader();
    const decoder = new TextDecoder('utf-8');

    if (!reader) {
        throw new Error('Streaming not supported');
    }

    let buffer = '';
    
    while (true) {
        const { value, done } = await reader.read();
        if (done) break;
        
        buffer += decoder.decode(value, { stream: true });
        
        // Process SSE lines
        const lines = buffer.split('\n\n');
        // Keep the last incomplete chunk in the buffer
        buffer = lines.pop() || '';
        
        for (const line of lines) {
            if (line.startsWith('data: ')) {
                const dataStr = line.substring(6);
                try {
                    const data = JSON.parse(dataStr);
                    if (data.type === 'TEXT') {
                        onChunk(data.content);
                    } else if (data.type === 'ERROR') {
                        console.error('AI Stream Error:', data.error);
                        throw new Error(data.error);
                    }
                } catch (e) {
                    console.error('Failed to parse stream chunk', dataStr, e);
                }
            }
        }
    }
};

export const deleteChatSession = (sessionId: number) =>
    http.delete(`/api/chat/sessions/${sessionId}`).then((res) => res.data);
