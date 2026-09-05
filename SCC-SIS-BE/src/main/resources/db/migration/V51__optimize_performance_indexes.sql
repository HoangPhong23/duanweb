-- V51: Thêm index tối ưu hiệu năng cho các truy vấn nghẽn
-- Khắc phục lỗi Full Table Scan tại trang Hồ sơ học viên (~40s → <0.1s)

-- =====================================================
-- 1. ENROLLMENTS: Tối ưu truy vấn theo student + status + revoked
-- Hỗ trợ query: findByStudent_StudentIdAndStatusAndRevokedAtIsNull
-- Hỗ trợ query: findByStudent_StudentIdAndRevokedAtIsNull
-- =====================================================
CREATE INDEX idx_enrollments_student_status_revoked
    ON enrollments (student_id, status, revoked_at)
    COMMENT 'Tối ưu tìm enrollment theo học viên + trạng thái + chưa bị hủy';

-- =====================================================
-- 2. ENROLLMENTS: Tối ưu truy vấn theo student + class
-- Hỗ trợ query: findByStudent_StudentIdAndClassEntity_ClassId
-- =====================================================
CREATE INDEX idx_enrollments_student_class
    ON enrollments (student_id, class_id)
    COMMENT 'Tối ưu tìm enrollment theo học viên + lớp cụ thể';

-- =====================================================
-- 3. ENROLLMENTS: Tối ưu truy vấn đếm học viên active theo lớp
-- Hỗ trợ query: findByClassEntity_ClassIdAndDeletedFalse (đếm sĩ số)
-- Mở rộng từ idx_enrollments_class_status thêm revoked_at
-- =====================================================
CREATE INDEX idx_enrollments_class_revoked
    ON enrollments (class_id, revoked_at)
    COMMENT 'Tối ưu đếm sĩ số lớp (chỉ enrollment chưa bị hủy)';

-- =====================================================
-- 4. ATTENDANCE_RECORDS: Tối ưu truy vấn lịch sử điểm danh
-- Hỗ trợ query: findByStudentIdAndClassIdOrderByAttendanceDateDesc
-- Bổ sung cột deleted vào index student_id
-- =====================================================
CREATE INDEX idx_attendance_records_student_deleted
    ON attendance_records (student_id, deleted)
    COMMENT 'Tối ưu lọc điểm danh theo học viên + chưa bị xóa';

-- =====================================================
-- 5. CLASS_TEACHERS: Tối ưu truy vấn giảng viên theo lớp
-- Hỗ trợ query: findActiveByClassIdWithSearch (trang ClassesPage)
-- =====================================================
CREATE INDEX idx_class_teachers_class_enddate
    ON class_teachers (class_id, end_date)
    COMMENT 'Tối ưu tìm giảng viên active theo lớp';

CREATE INDEX idx_class_teachers_teacher_enddate
    ON class_teachers (teacher_id, end_date)
    COMMENT 'Tối ưu tìm lớp của giảng viên';
