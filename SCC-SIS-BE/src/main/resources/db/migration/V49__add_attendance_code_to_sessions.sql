-- V49__add_attendance_code_to_sessions.sql
-- Thêm cột hỗ trợ điểm danh bằng mã

ALTER TABLE attendance_sessions
    ADD COLUMN attendance_code VARCHAR(20) NULL COMMENT 'Mã điểm danh do giảng viên đặt',
    ADD COLUMN code_enabled BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'Bật/tắt điểm danh bằng mã',
    ADD COLUMN code_expires_at DATETIME NULL COMMENT 'Thời điểm hết hạn mã (NULL = không giới hạn)';

-- Index để tra cứu nhanh theo mã
CREATE INDEX idx_attendance_sessions_code
    ON attendance_sessions (attendance_code, code_enabled)
    COMMENT 'Tra cứu buổi điểm danh theo mã';
