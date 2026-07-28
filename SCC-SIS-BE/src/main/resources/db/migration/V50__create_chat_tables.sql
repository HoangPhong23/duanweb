-- Migration V50: Create Chat tables for 1-1 and Group Chat, attachments, and messaging

CREATE TABLE IF NOT EXISTS chat_rooms (
    room_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_type VARCHAR(20) NOT NULL COMMENT 'DIRECT or CLASS_GROUP',
    class_id INT NULL COMMENT 'FK to classes.class_id if CLASS_GROUP',
    name VARCHAR(255) NULL COMMENT 'Room name (e.g. class name or direct chat label)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_rooms_class FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_participants (
    participant_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    user_id INT NOT NULL,
    last_read_at DATETIME NULL,
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_chat_participants_room FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_participants_user FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY uk_room_user (room_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS user_chat_messages (
    message_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    sender_id INT NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT, IMAGE, FILE',
    content TEXT NULL COMMENT 'Text content or caption',
    attachment_url VARCHAR(512) NULL COMMENT 'File or Image URL',
    file_name VARCHAR(255) NULL COMMENT 'Original file name',
    file_size BIGINT NULL COMMENT 'File size in bytes',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_chat_messages_room FOREIGN KEY (room_id) REFERENCES chat_rooms(room_id) ON DELETE CASCADE,
    CONSTRAINT fk_user_chat_messages_sender FOREIGN KEY (sender_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Indexes for fast querying
CREATE INDEX idx_user_chat_messages_room_time ON user_chat_messages(room_id, created_at DESC);
CREATE INDEX idx_chat_participants_user ON chat_participants(user_id);
