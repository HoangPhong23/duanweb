import React, { useState, useEffect, useRef, useMemo } from 'react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import EmojiPicker, { type EmojiClickData } from 'emoji-picker-react';
import {
    Send, Image, Paperclip, Smile, Search, Users, User, MessageSquare,
    CheckCheck, FileText, Download, Loader2, RefreshCw, X, Plus, UserPlus
} from 'lucide-react';
import { useUserProfile } from '@/stores/userProfile';
import { useToast } from '@/shared/hooks/useToast';
import {
    getUserChatRooms, getRoomMessages, sendMessageApi, uploadChatAttachment,
    markRoomAsRead, getOrCreateDirectRoom, type ChatRoom, type ChatMessage
} from '@/shared/api/chat';
import { listUsers } from '@/shared/api/users';

function formatFileSize(bytes?: number) {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(1)) + ' ' + sizes[i];
}

function formatTime(dateStr?: string) {
    if (!dateStr) return '';
    const date = new Date(dateStr);
    return date.toLocaleTimeString('vi-VN', { hour: '2-digit', minute: '2-digit' });
}

export default function ChatPage() {
    const { me } = useUserProfile();
    const toast = useToast();

    const [rooms, setRooms] = useState<ChatRoom[]>([]);
    const [selectedRoom, setSelectedRoom] = useState<ChatRoom | null>(null);
    const [messages, setMessages] = useState<ChatMessage[]>([]);
    const [textInput, setTextInput] = useState('');
    const [searchQuery, setSearchQuery] = useState('');
    const [activeFilter, setActiveFilter] = useState<'ALL' | 'CLASS' | 'DIRECT'>('ALL');
    const [showEmoji, setShowEmoji] = useState(false);
    const [loadingRooms, setLoadingRooms] = useState(true);
    const [loadingMessages, setLoadingMessages] = useState(false);
    const [uploadingFile, setUploadingFile] = useState(false);
    const [selectedImageModal, setSelectedImageModal] = useState<string | null>(null);

    const messagesEndRef = useRef<HTMLDivElement>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const imageInputRef = useRef<HTMLInputElement>(null);
    const stompClientRef = useRef<Client | null>(null);

    // Load rooms list
    useEffect(() => {
        loadChatRooms();
    }, []);

    const loadChatRooms = async () => {
        try {
            setLoadingRooms(true);
            const res = await getUserChatRooms();
            setRooms(res.data);
            if (res.data.length > 0 && !selectedRoom) {
                setSelectedRoom(res.data[0]);
            }
        } catch (e) {
            toast.error('Không thể tải danh sách cuộc trò chuyện');
        } finally {
            setLoadingRooms(false);
        }
    };

    // Load messages when active room changes
    useEffect(() => {
        if (!selectedRoom) return;

        const fetchMessages = async () => {
            try {
                setLoadingMessages(true);
                const res = await getRoomMessages(selectedRoom.roomId);
                setMessages(res.data);
                markRoomAsRead(selectedRoom.roomId);
                
                // Clear unread count locally
                setRooms(prev => prev.map(r => r.roomId === selectedRoom.roomId ? { ...r, unreadCount: 0 } : r));
            } catch (e) {
                toast.error('Không thể tải tin nhắn');
            } finally {
                setLoadingMessages(false);
                scrollToBottom();
            }
        };

        fetchMessages();

        // Connect STOMP WebSocket
        const client = new Client({
            brokerURL: 'ws://localhost:7001/ws-chat/websocket',
            webSocketFactory: () => new SockJS('http://localhost:7001/ws-chat'),
            reconnectDelay: 5000,
            onConnect: () => {
                client.subscribe(`/topic/chat/${selectedRoom.roomId}`, (message) => {
                    const receivedMsg: ChatMessage = JSON.parse(message.body);
                    setMessages(prev => {
                        if (prev.some(m => m.messageId === receivedMsg.messageId)) return prev;
                        return [...prev, receivedMsg];
                    });
                    scrollToBottom();
                    markRoomAsRead(selectedRoom.roomId);
                });
            },
        });

        client.activate();
        stompClientRef.current = client;

        return () => {
            if (client) client.deactivate();
        };
    }, [selectedRoom?.roomId]);

    const scrollToBottom = () => {
        setTimeout(() => {
            messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
        }, 100);
    };

    // Filter rooms
    const filteredRooms = useMemo(() => {
        return rooms.filter(r => {
            const matchesQuery = r.roomName.toLowerCase().includes(searchQuery.toLowerCase());
            if (activeFilter === 'CLASS') return matchesQuery && r.roomType === 'CLASS_GROUP';
            if (activeFilter === 'DIRECT') return matchesQuery && r.roomType === 'DIRECT';
            return matchesQuery;
        });
    }, [rooms, searchQuery, activeFilter]);

    // Send text message
    const handleSendText = async () => {
        if (!textInput.trim() || !selectedRoom) return;

        const content = textInput.trim();
        setTextInput('');
        setShowEmoji(false);

        try {
            await sendMessageApi({
                roomId: selectedRoom.roomId,
                messageType: 'TEXT',
                content,
            });
        } catch (e) {
            toast.error('Lỗi khi gửi tin nhắn');
        }
    };

    // Attach File / Image
    const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>, isImage: boolean) => {
        const file = e.target.files?.[0];
        if (!file || !selectedRoom) return;

        try {
            setUploadingFile(true);
            const res = await uploadChatAttachment(file);
            const data = res.data;

            await sendMessageApi({
                roomId: selectedRoom.roomId,
                messageType: isImage ? 'IMAGE' : 'FILE',
                attachmentUrl: data.url,
                fileName: data.fileName,
                fileSize: Number(data.fileSize),
            });
        } catch (err) {
            toast.error('Lỗi khi đính kèm tệp');
        } finally {
            setUploadingFile(false);
            if (e.target) e.target.value = '';
        }
    };

    const handleEmojiClick = (emojiData: EmojiClickData) => {
        setTextInput(prev => prev + emojiData.emoji);
    };

    // Modal tạo cuộc trò chuyện mới
    const [showNewChatModal, setShowNewChatModal] = useState(false);
    const [userList, setUserList] = useState<any[]>([]);
    const [userSearchQuery, setUserSearchQuery] = useState('');
    const [loadingUsers, setLoadingUsers] = useState(false);

    const handleOpenNewChatModal = async () => {
        setShowNewChatModal(true);
        try {
            setLoadingUsers(true);
            const res = await listUsers();
            setUserList(res.data || []);
        } catch (e) {
            toast.error('Không thể nạp danh sách người dùng');
        } finally {
            setLoadingUsers(false);
        }
    };

    const handleStartDirectChatWithUser = async (targetUserId: number) => {
        try {
            const res = await getOrCreateDirectRoom(targetUserId);
            const newRoom = res.data;
            setShowNewChatModal(false);
            setRooms(prev => {
                if (prev.some(r => r.roomId === newRoom.roomId)) return prev;
                return [newRoom, ...prev];
            });
            setSelectedRoom(newRoom);
        } catch (e) {
            toast.error('Không thể tạo trò chuyện với người dùng này');
        }
    };

    const filteredUserList = useMemo(() => {
        return userList.filter(u => 
            u.userId !== me?.userId &&
            (u.fullName?.toLowerCase().includes(userSearchQuery.toLowerCase()) ||
             u.email?.toLowerCase().includes(userSearchQuery.toLowerCase()) ||
             u.phone?.toLowerCase().includes(userSearchQuery.toLowerCase()))
        );
    }, [userList, userSearchQuery, me?.userId]);

    return (
        <div className="h-[calc(100vh-5rem)] flex bg-white rounded-xl border border-gray-200 shadow-sm overflow-hidden">
            
            {/* ===== CỘT TRÁI: DANH SÁCH PHÒNG CHAT ===== */}
            <div className="w-80 md:w-96 border-r border-gray-200 flex flex-col bg-gray-50/50">
                
                {/* Header phòng chat */}
                <div className="p-4 border-b border-gray-200 bg-white">
                    <div className="flex items-center justify-between mb-3">
                        <h1 className="text-xl font-bold text-gray-900 flex items-center gap-2">
                            <MessageSquare className="w-6 h-6 text-indigo-600" />
                            Tin nhắn
                        </h1>
                        <button
                            onClick={handleOpenNewChatModal}
                            className="px-2.5 py-1.5 bg-indigo-50 hover:bg-indigo-100 text-indigo-600 rounded-lg flex items-center gap-1.5 text-xs font-semibold transition-colors"
                            title="Tạo trò chuyện mới"
                        >
                            <UserPlus size={16} />
                            <span>Nhắn tin mới</span>
                        </button>
                    </div>

                    {/* Ô Tìm kiếm */}
                    <div className="relative">
                        <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                        <input
                            type="text"
                            placeholder="Tìm kiếm trò chuyện..."
                            value={searchQuery}
                            onChange={(e) => setSearchQuery(e.target.value)}
                            className="w-full pl-9 pr-4 py-2 bg-gray-100 rounded-lg text-sm border-0 focus:ring-2 focus:ring-indigo-500 outline-none transition-all"
                        />
                    </div>

                    {/* Filter Tabs */}
                    <div className="flex gap-1 mt-3 bg-gray-100 p-1 rounded-lg">
                        <button
                            onClick={() => setActiveFilter('ALL')}
                            className={`flex-1 py-1 text-xs font-semibold rounded-md transition-all ${
                                activeFilter === 'ALL' ? 'bg-white text-gray-900 shadow-xs' : 'text-gray-500 hover:text-gray-700'
                            }`}
                        >
                            Tất cả
                        </button>
                        <button
                            onClick={() => setActiveFilter('CLASS')}
                            className={`flex-1 py-1 text-xs font-semibold rounded-md transition-all ${
                                activeFilter === 'CLASS' ? 'bg-white text-indigo-700 shadow-xs' : 'text-gray-500 hover:text-gray-700'
                            }`}
                        >
                            Nhóm lớp
                        </button>
                        <button
                            onClick={() => setActiveFilter('DIRECT')}
                            className={`flex-1 py-1 text-xs font-semibold rounded-md transition-all ${
                                activeFilter === 'DIRECT' ? 'bg-white text-indigo-700 shadow-xs' : 'text-gray-500 hover:text-gray-700'
                            }`}
                        >
                            Cá nhân
                        </button>
                    </div>
                </div>

                {/* Danh sách phòng */}
                <div className="flex-1 overflow-y-auto divide-y divide-gray-100">
                    {loadingRooms ? (
                        <div className="text-center py-8 text-xs text-gray-400">Đang tải cuộc trò chuyện...</div>
                    ) : filteredRooms.length === 0 ? (
                        <div className="text-center py-12 text-xs text-gray-400">Chưa có cuộc trò chuyện nào</div>
                    ) : (
                        filteredRooms.map((room) => {
                            const isSelected = selectedRoom?.roomId === room.roomId;
                            const isClassGroup = room.roomType === 'CLASS_GROUP';

                            return (
                                <div
                                    key={room.roomId}
                                    onClick={() => setSelectedRoom(room)}
                                    className={`p-3.5 flex items-center gap-3 cursor-pointer transition-colors relative ${
                                        isSelected ? 'bg-indigo-50/70 border-l-4 border-indigo-600' : 'hover:bg-gray-100/70 bg-white'
                                    }`}
                                >
                                    {/* Avatar */}
                                    <div className="relative flex-shrink-0">
                                        <div className={`w-12 h-12 rounded-full flex items-center justify-center font-bold text-white shadow-xs ${
                                            isClassGroup ? 'bg-gradient-to-br from-indigo-500 to-purple-600' : 'bg-gradient-to-br from-blue-500 to-indigo-600'
                                        }`}>
                                            {isClassGroup ? <Users size={20} /> : room.roomName.charAt(0).toUpperCase()}
                                        </div>
                                    </div>

                                    {/* Nội dung tin nhắn cuối */}
                                    <div className="flex-1 min-w-0">
                                        <div className="flex justify-between items-baseline mb-1">
                                            <h4 className="text-sm font-semibold text-gray-900 truncate">{room.roomName}</h4>
                                            {room.lastMessageTime && (
                                                <span className="text-[11px] text-gray-400 whitespace-nowrap ml-2">
                                                    {formatTime(room.lastMessageTime)}
                                                </span>
                                            )}
                                        </div>
                                        <p className="text-xs text-gray-500 truncate font-normal">
                                            {room.lastMessage || 'Chưa có tin nhắn'}
                                        </p>
                                    </div>

                                    {/* Count chưa đọc */}
                                    {room.unreadCount > 0 && (
                                        <span className="bg-red-500 text-white text-[10px] font-bold px-2 py-0.5 rounded-full shadow-xs">
                                            {room.unreadCount}
                                        </span>
                                    )}
                                </div>
                            );
                        })
                    )}
                </div>
            </div>

            {/* ===== CỘT PHẢI: KHUNG CHAT CHÍNH ===== */}
            {selectedRoom ? (
                <div className="flex-1 flex flex-col bg-white">
                    {/* Header khung chat */}
                    <div className="px-6 py-3.5 border-b border-gray-200 flex items-center justify-between bg-white">
                        <div className="flex items-center gap-3">
                            <div className={`w-10 h-10 rounded-full flex items-center justify-center font-bold text-white ${
                                selectedRoom.roomType === 'CLASS_GROUP' ? 'bg-indigo-600' : 'bg-blue-600'
                            }`}>
                                {selectedRoom.roomType === 'CLASS_GROUP' ? <Users size={18} /> : selectedRoom.roomName.charAt(0).toUpperCase()}
                            </div>
                            <div>
                                <h2 className="text-base font-bold text-gray-900">{selectedRoom.roomName}</h2>
                                <span className="text-xs text-green-600 font-medium flex items-center gap-1">
                                    <span className="w-2 h-2 rounded-full bg-green-500 inline-block animate-pulse" />
                                    {selectedRoom.roomType === 'CLASS_GROUP' ? 'Nhóm lớp học' : 'Đang hoạt động'}
                                </span>
                            </div>
                        </div>
                    </div>

                    {/* Vùng Tin nhắn (Messages) */}
                    <div className="flex-1 overflow-y-auto p-6 space-y-4 bg-gray-50/30">
                        {loadingMessages ? (
                            <div className="text-center py-12 text-xs text-gray-400">Đang nạp tin nhắn...</div>
                        ) : messages.length === 0 ? (
                            <div className="text-center py-16 text-gray-400 text-xs">
                                Chưa có tin nhắn nào trong cuộc trò chuyện này. Hãy gửi tin nhắn đầu tiên!
                            </div>
                        ) : (
                            messages.map((msg) => {
                                const isMe = msg.senderId === me?.userId;

                                return (
                                    <div key={msg.messageId} className={`flex items-end gap-2.5 ${isMe ? 'flex-row-reverse' : 'flex-row'}`}>
                                        {/* Avatar đối phương */}
                                        {!isMe && (
                                            <div className="w-8 h-8 rounded-full bg-gray-300 flex items-center justify-center font-bold text-gray-700 text-xs flex-shrink-0">
                                                {msg.senderName ? msg.senderName.charAt(0).toUpperCase() : 'U'}
                                            </div>
                                        )}

                                        <div className={`max-w-[70%] space-y-1 ${isMe ? 'items-end text-right' : 'items-start text-left'}`}>
                                            {!isMe && (
                                                <span className="text-[11px] font-semibold text-gray-500 block px-1">
                                                    {msg.senderName}
                                                </span>
                                            )}

                                            {/* Bong bóng tin nhắn */}
                                            <div className={`p-3 rounded-2xl text-sm leading-relaxed shadow-xs inline-block text-left ${
                                                isMe ? 'bg-indigo-600 text-white rounded-br-none' : 'bg-white border border-gray-200 text-gray-800 rounded-bl-none'
                                            }`}>
                                                {/* Text message */}
                                                {msg.messageType === 'TEXT' && <span>{msg.content}</span>}

                                                {/* Image message */}
                                                {msg.messageType === 'IMAGE' && (
                                                    <div className="space-y-1">
                                                        <img
                                                            src={`http://localhost:7001${msg.attachmentUrl}`}
                                                            alt="Chat Image"
                                                            onClick={() => setSelectedImageModal(`http://localhost:7001${msg.attachmentUrl}`)}
                                                            className="max-w-xs max-h-60 rounded-lg cursor-pointer hover:opacity-90 transition-opacity object-cover"
                                                        />
                                                        {msg.content && <p className="text-xs mt-1">{msg.content}</p>}
                                                    </div>
                                                )}

                                                {/* File message */}
                                                {msg.messageType === 'FILE' && (
                                                    <div className="flex items-center gap-3">
                                                        <div className={`p-2 rounded-lg ${isMe ? 'bg-indigo-700' : 'bg-indigo-50 text-indigo-600'}`}>
                                                            <FileText size={20} />
                                                        </div>
                                                        <div className="flex-1 min-w-0">
                                                            <p className="font-semibold text-xs truncate max-w-[160px]">{msg.fileName || 'Tệp đính kèm'}</p>
                                                            <span className="text-[10px] opacity-75">{formatFileSize(msg.fileSize)}</span>
                                                        </div>
                                                        <a
                                                            href={`http://localhost:7001${msg.attachmentUrl}`}
                                                            download={msg.fileName}
                                                            target="_blank"
                                                            rel="noopener noreferrer"
                                                            className={`p-1.5 rounded-md transition-colors ${
                                                                isMe ? 'hover:bg-indigo-700 text-white' : 'hover:bg-gray-100 text-gray-600'
                                                            }`}
                                                            title="Tải về"
                                                        >
                                                            <Download size={16} />
                                                        </a>
                                                    </div>
                                                )}
                                            </div>

                                            {/* Thời gian */}
                                            <div className={`text-[10px] text-gray-400 px-1 ${isMe ? 'text-right' : 'text-left'}`}>
                                                {formatTime(msg.createdAt)}
                                            </div>
                                        </div>
                                    </div>
                                );
                            })
                        )}
                        <div ref={messagesEndRef} />
                    </div>

                    {/* Emoji Picker Popover */}
                    {showEmoji && (
                        <div className="absolute bottom-20 right-12 z-50 shadow-2xl rounded-2xl overflow-hidden border">
                            <EmojiPicker onEmojiClick={handleEmojiClick} searchDisabled={false} width={320} height={400} />
                        </div>
                    )}

                    {/* Thanh Công Cụ Nhắn Tin */}
                    <div className="p-3.5 border-t border-gray-200 bg-white flex items-center gap-2 relative">
                        {/* Hidden File Inputs */}
                        <input
                            type="file"
                            ref={imageInputRef}
                            accept="image/*"
                            onChange={(e) => handleFileUpload(e, true)}
                            className="hidden"
                        />
                        <input
                            type="file"
                            ref={fileInputRef}
                            onChange={(e) => handleFileUpload(e, false)}
                            className="hidden"
                        />

                        {/* Nút Upload Ảnh */}
                        <button
                            onClick={() => imageInputRef.current?.click()}
                            disabled={uploadingFile}
                            className="p-2 text-gray-500 hover:text-indigo-600 hover:bg-gray-100 rounded-lg transition-colors"
                            title="Gửi hình ảnh"
                        >
                            <Image size={20} />
                        </button>

                        {/* Nút Upload File */}
                        <button
                            onClick={() => fileInputRef.current?.click()}
                            disabled={uploadingFile}
                            className="p-2 text-gray-500 hover:text-indigo-600 hover:bg-gray-100 rounded-lg transition-colors"
                            title="Đính kèm tệp"
                        >
                            <Paperclip size={20} />
                        </button>

                        {/* Nút Emoji */}
                        <button
                            onClick={() => setShowEmoji(!showEmoji)}
                            className="p-2 text-gray-500 hover:text-amber-500 hover:bg-gray-100 rounded-lg transition-colors"
                            title="Biểu cảm Emoji"
                        >
                            <Smile size={20} />
                        </button>

                        {/* Input gõ nội dung */}
                        <input
                            type="text"
                            value={textInput}
                            onChange={(e) => setTextInput(e.target.value)}
                            onKeyDown={(e) => e.key === 'Enter' && handleSendText()}
                            placeholder={uploadingFile ? 'Đang tải file lên...' : 'Nhập tin nhắn...'}
                            disabled={uploadingFile}
                            className="flex-1 px-4 py-2.5 bg-gray-100 border-0 rounded-xl text-sm focus:ring-2 focus:ring-indigo-500 outline-none transition-all"
                        />

                        {/* Nút Gửi */}
                        <button
                            onClick={handleSendText}
                            disabled={!textInput.trim() || uploadingFile}
                            className="p-2.5 bg-indigo-600 text-white rounded-xl hover:bg-indigo-700 disabled:opacity-40 disabled:cursor-not-allowed transition-all shadow-xs"
                            title="Gửi tin nhắn"
                        >
                            {uploadingFile ? <Loader2 size={18} className="animate-spin" /> : <Send size={18} />}
                        </button>
                    </div>
                </div>
            ) : (
                <div className="flex-1 flex flex-col items-center justify-center text-gray-400 bg-gray-50/30">
                    <MessageSquare size={48} className="text-gray-300 mb-3" />
                    <p className="text-sm font-medium">Chọn một cuộc trò chuyện để bắt đầu nhắn tin</p>
                </div>
            )}

            {/* Modal Xem Ảnh Phóng To */}
            {selectedImageModal && (
                <div
                    onClick={() => setSelectedImageModal(null)}
                    className="fixed inset-0 z-50 bg-black/80 flex items-center justify-center p-4 cursor-pointer"
                >
                    <div className="relative max-w-4xl max-h-[90vh]">
                        <img src={selectedImageModal} alt="Enlarged" className="max-w-full max-h-[90vh] rounded-lg shadow-2xl object-contain" />
                        <button
                            onClick={() => setSelectedImageModal(null)}
                            className="absolute top-2 right-2 bg-black/60 text-white p-1.5 rounded-full hover:bg-black"
                        >
                            <X size={20} />
                        </button>
                    </div>
                </div>
            )}

            {/* Modal Tạo trò chuyện mới 1-1 */}
            {showNewChatModal && (
                <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4">
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md overflow-hidden flex flex-col max-h-[85vh]">
                        <div className="px-6 py-4 border-b border-gray-100 flex justify-between items-center bg-gray-50/50">
                            <h3 className="font-bold text-gray-900 flex items-center gap-2">
                                <UserPlus className="w-5 h-5 text-indigo-600" />
                                Bắt đầu trò chuyện mới
                            </h3>
                            <button
                                onClick={() => setShowNewChatModal(false)}
                                className="p-1 rounded-lg hover:bg-gray-200 text-gray-500"
                            >
                                <X size={18} />
                            </button>
                        </div>

                        <div className="p-4 border-b border-gray-100">
                            <div className="relative">
                                <Search className="w-4 h-4 text-gray-400 absolute left-3 top-1/2 -translate-y-1/2" />
                                <input
                                    type="text"
                                    placeholder="Tìm tên, email hoặc số điện thoại..."
                                    value={userSearchQuery}
                                    onChange={(e) => setUserSearchQuery(e.target.value)}
                                    className="w-full pl-9 pr-4 py-2 bg-gray-100 rounded-lg text-sm border-0 focus:ring-2 focus:ring-indigo-500 outline-none"
                                    autoFocus
                                />
                            </div>
                        </div>

                        <div className="flex-1 overflow-y-auto divide-y divide-gray-100 p-2">
                            {loadingUsers ? (
                                <div className="text-center py-8 text-xs text-gray-400">Đang tìm kiếm người dùng...</div>
                            ) : filteredUserList.length === 0 ? (
                                <div className="text-center py-8 text-xs text-gray-400">Không tìm thấy người dùng nào</div>
                            ) : (
                                filteredUserList.map((user) => (
                                    <div
                                        key={user.userId}
                                        onClick={() => handleStartDirectChatWithUser(user.userId)}
                                        className="p-3 flex items-center gap-3 rounded-xl hover:bg-indigo-50/60 cursor-pointer transition-colors"
                                    >
                                        <div className="w-10 h-10 rounded-full bg-gradient-to-br from-indigo-500 to-purple-600 text-white font-bold flex items-center justify-center text-sm shadow-xs flex-shrink-0">
                                            {user.fullName ? user.fullName.charAt(0).toUpperCase() : 'U'}
                                        </div>
                                        <div className="flex-1 min-w-0">
                                            <h4 className="text-sm font-semibold text-gray-900 truncate">{user.fullName}</h4>
                                            <p className="text-xs text-gray-500 truncate">{user.email || user.phone}</p>
                                        </div>
                                        <button className="px-3 py-1 bg-indigo-600 text-white text-xs font-semibold rounded-lg hover:bg-indigo-700">
                                            Nhắn tin
                                        </button>
                                    </div>
                                ))
                            )}
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
