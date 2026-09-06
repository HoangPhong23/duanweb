package com.example.sis.repositories;

import com.example.sis.models.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<Student, Integer> {

  boolean existsByEmail(String email);

  /**
   * Kiểm tra email đã tồn tại (case-insensitive, không bao gồm học viên đã xóa mềm)
   */
  @Query("""
      SELECT COUNT(s) > 0
      FROM Student s
      WHERE LOWER(s.email) = LOWER(:email)
        AND s.deletedAt IS NULL
      """)
  boolean existsByEmailIgnoreCase(@Param("email") String email);

  /**
   * Kiểm tra hồ sơ học viên có hợp lệ (đang hoạt động, chưa bị xóa mềm)
   */
  @Query("""
      SELECT COUNT(s) > 0
      FROM Student s
      WHERE s.studentId = :studentId
        AND s.deletedAt IS NULL
      """)
  boolean isUsableProfile(Integer studentId);

  /**
   * Tìm kiếm học viên theo tên hoặc email (không bao gồm học viên đã xóa mềm)
   */
  @Query("""
      SELECT s FROM Student s
      WHERE s.deletedAt IS NULL
        AND (LOWER(s.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
         OR LOWER(s.email) LIKE LOWER(CONCAT('%', :keyword, '%')))
      ORDER BY s.fullName ASC
      """)
  List<Student> searchByNameOrEmail(@Param("keyword") String keyword);

  /**
   * Lấy tất cả học viên chưa bị xóa mềm
   */
  @Query("SELECT s FROM Student s WHERE s.deletedAt IS NULL ORDER BY s.createdAt DESC")
  List<Student> findAllActiveStudents();

  /**
   * Tìm studentId từ userId
   */
  @Query("SELECT s.studentId FROM Student s WHERE s.user.userId = :userId AND s.deletedAt IS NULL")
  Integer findStudentIdByUserId(@Param("userId") Integer userId);

  /**
   * Lấy thông tin enrollments của học viên với chi tiết lớp và chương trình
   */
  @Query("""
      SELECT e.enrollmentId, e.classEntity.classId, c.name, c.program.name, CAST(e.status AS string), e.enrolledAt, e.leftAt, e.note
      FROM Enrollment e
      JOIN e.classEntity c
      WHERE e.student.studentId = :studentId AND e.revokedAt IS NULL
      ORDER BY e.enrolledAt DESC
      """)
  List<Object[]> findEnrollmentsByStudentId(@Param("studentId") Integer studentId);

  /**
   * Tìm student theo userId (không bao gồm đã xóa mềm)
   */
  @Query("SELECT s FROM Student s WHERE s.user.userId = :userId AND s.deletedAt IS NULL")
  java.util.Optional<Student> findByUserIdAndDeletedAtIsNull(@Param("userId") Integer userId);

  @Query(value = """
      SELECT s.student_id AS studentId,
             s.email AS code,
             s.full_name AS name,
             c.name AS classCode,
             p.name AS program,
             COUNT(DISTINCT CASE WHEN ar.status = 'ABSENT' THEN ar.record_id END) AS absentCount,
             COUNT(DISTINCT CASE WHEN gr.pass_status = 'FAIL' THEN gr.record_id END) AS failCount
      FROM students s
      JOIN enrollments e ON e.student_id = s.student_id AND e.status = 'ACTIVE' AND e.revoked_at IS NULL
      JOIN classes c ON c.class_id = e.class_id AND c.deleted_at IS NULL
      LEFT JOIN programs p ON p.program_id = c.program_id
      LEFT JOIN attendance_records ar ON ar.student_id = s.student_id AND ar.deleted = false
      LEFT JOIN grade_records gr ON gr.student_id = s.student_id
      WHERE (:centerId IS NULL OR c.center_id = :centerId) AND s.deleted_at IS NULL
      GROUP BY s.student_id, s.email, s.full_name, c.name, p.name
      HAVING COUNT(DISTINCT CASE WHEN ar.status = 'ABSENT' THEN ar.record_id END) > 2 
          OR COUNT(DISTINCT CASE WHEN gr.pass_status = 'FAIL' THEN gr.record_id END) > 2
      """, nativeQuery = true)
  List<Object[]> findStudentWarningsAggregate(@Param("centerId") Integer centerId);
}
