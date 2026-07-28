package com.example.sis.service.system;

import com.example.sis.repositories.UserRepository;
import com.example.sis.repositories.ClassRepository;
import com.example.sis.repositories.EnrollmentRepository;
import com.example.sis.repositories.LessonProgressRepository;
import com.example.sis.repositories.ModuleRepository;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.models.ClassEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemStatsService {
    
    private final UserRepository userRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final ClassRepository classRepository;
    private final ModuleRepository moduleRepository;
    private final LessonProgressRepository lessonProgressRepository;
    private final UserRoleRepository userRoleRepository;
    
    @PersistenceContext
    private EntityManager entityManager;
    
    /**
     * Get total number of users in system
     */
    public Long getTotalUsers() {
        return userRepository.count();
    }
    
    /**
     * Get total number of students (users with STUDENT role)
     */
    public Long getTotalStudents() {
        String jpql = "SELECT COUNT(DISTINCT ur.user.userId) FROM UserRole ur " +
                     "JOIN ur.role r WHERE r.code = 'STUDENT' AND ur.revokedAt IS NULL";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }
    
    /**
     * Get total number of admins
     */
    public Long getTotalAdmins() {
        String jpql = "SELECT COUNT(DISTINCT ur.user.userId) FROM UserRole ur " +
                     "JOIN ur.role r WHERE r.code IN ('SUPER_ADMIN', 'CENTER_MANAGER') AND ur.revokedAt IS NULL";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }
    
    /**
     * Get total number of instructors
     */
    public Long getTotalInstructors() {
        String jpql = "SELECT COUNT(DISTINCT ur.user.userId) FROM UserRole ur " +
                     "JOIN ur.role r WHERE r.code = 'LECTURER' AND ur.revokedAt IS NULL";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }
    
    /**
     * Get active students (enrolled in at least one class)
     */
    public Long getActiveStudents() {
        String jpql = "SELECT COUNT(DISTINCT e.student.studentId) FROM Enrollment e " +
                     "WHERE e.status = 'ACTIVE' AND e.revokedAt IS NULL";
        return entityManager.createQuery(jpql, Long.class).getSingleResult();
    }
    
    /**
     * Get class start date and calculate days until start
     * @param className Class name (e.g., "Java K-17")
     * @return Map with start_date, days_until_start, status
     */
    public Map<String, Object> getClassStartInfo(String className) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Query class by name
            String jpql = "SELECT c FROM ClassEntity c WHERE c.name = :className AND c.deletedAt IS NULL";
            ClassEntity classEntity = entityManager.createQuery(jpql, ClassEntity.class)
                    .setParameter("className", className)
                    .getSingleResult();
            
            LocalDate startDate = classEntity.getStartDate();
            // ⚠️ CRITICAL: Use Vietnam timezone (GMT+7) instead of server timezone
            LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            
            log.info("🕐 Vietnam time today: {}, Class start date: {}", today, startDate);
            
            long daysUntilStart = ChronoUnit.DAYS.between(today, startDate);
            
            result.put("class_name", className);
            result.put("start_date", startDate.toString());
            result.put("days_until_start", daysUntilStart);
            
            if (daysUntilStart > 0) {
                result.put("status", "upcoming");
                result.put("message", String.format("Lớp sẽ bắt đầu sau %d ngày", daysUntilStart));
            } else if (daysUntilStart == 0) {
                result.put("status", "starting_today");
                result.put("message", "Lớp bắt đầu hôm nay");
            } else {
                result.put("status", "started");
                result.put("message", String.format("Lớp đã bắt đầu được %d ngày", Math.abs(daysUntilStart)));
            }
            
            result.put("success", true);
            
        } catch (jakarta.persistence.NoResultException e) {
            log.warn("Class not found: {}", className);
            result.put("success", false);
            result.put("error", String.format("Không tìm thấy lớp '%s'", className));
        } catch (Exception e) {
            log.error("Failed to get class start info for: {}", className, e);
            result.put("success", false);
            result.put("error", "Có lỗi xảy ra khi truy vấn thông tin lớp");
        }
        
        return result;
    }
    
    /**
     * Get comprehensive system statistics
     */
    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        
        stats.put("total_users", getTotalUsers());
        stats.put("total_students", getTotalStudents());
        stats.put("active_students", getActiveStudents());
        stats.put("total_admins", getTotalAdmins());
        stats.put("total_instructors", getTotalInstructors());
        stats.put("timestamp", LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).toString());
        
        return stats;
    }
    
    /**
     * Get enrollment statistics for a specific class
     */
    public Map<String, Object> getClassEnrollmentStats(String className) {
        Map<String, Object> stats = new HashMap<>();
        
        // TODO: Implement when ClassRepository is available
        // Long enrolled = enrollmentRepository.countByClassName(className);
        // Long capacity = classRepository.findByClassName(className).getCapacity();
        
        stats.put("class_name", className);
        stats.put("enrolled_students", 0L); // Replace with real data
        stats.put("capacity", 0L); // Replace with real data
        stats.put("available_slots", 0L); // Replace with real data
        
        return stats;
    }

    // ============================================================
    // LECTURER-SPECIFIC STATS
    // ============================================================

    /**
     * Get list of classes assigned to a lecturer (by userId)
     */
    public List<Map<String, Object>> getLecturerClasses(Integer userId) {
        String jpql = """
            SELECT c.classId, c.name, c.status, c.startDate, c.endDate,
                   c.room, c.studyTime,
                   p.name as programName,
                   ct.startDate as assignedFrom
            FROM ClassTeacher ct
            JOIN ct.classEntity c
            JOIN c.program p
            JOIN ct.teacher t
            WHERE t.userId = :userId
              AND (ct.endDate IS NULL OR ct.endDate >= CURRENT_DATE)
              AND c.deletedAt IS NULL
            ORDER BY c.name
            """;
        try {
            List<Object[]> rows = entityManager.createQuery(jpql)
                    .setParameter("userId", userId)
                    .getResultList();
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("classId",     row[0]);
                m.put("className",   row[1]);
                m.put("status",      row[2] != null ? row[2].toString() : "N/A");
                m.put("startDate",   row[3] != null ? row[3].toString() : "N/A");
                m.put("endDate",     row[4] != null ? row[4].toString() : "N/A");
                m.put("room",        row[5] != null ? row[5].toString() : "N/A");
                m.put("studyTime",   row[6] != null ? row[6].toString() : "N/A");
                m.put("programName", row[7] != null ? row[7].toString() : "N/A");
                result.add(m);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to get lecturer classes for userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * Get today's attendance summary for all classes of a lecturer
     */
    public List<Map<String, Object>> getLecturerTodayAttendance(Integer userId) {
        LocalDate today = LocalDate.now(java.time.ZoneId.of("Asia/Ho_Chi_Minh"));
        String jpql = """
            SELECT s.sessionId, c.classId, c.name,
                   SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN ar.status = 'ABSENT'  THEN 1 ELSE 0 END),
                   SUM(CASE WHEN ar.status = 'LATE'    THEN 1 ELSE 0 END),
                   COUNT(ar)
            FROM AttendanceSession s
            JOIN s.classEntity c
            JOIN c.classTeachers ct
            JOIN ct.teacher t
            LEFT JOIN s.records ar ON ar.deleted = false
            WHERE t.userId = :userId
              AND s.attendanceDate = :today
              AND (ct.endDate IS NULL OR ct.endDate >= CURRENT_DATE)
              AND c.deletedAt IS NULL
            GROUP BY s.sessionId, c.classId, c.name
            """;
        try {
            List<Object[]> rows = entityManager.createQuery(jpql)
                    .setParameter("userId", userId)
                    .setParameter("today", today)
                    .getResultList();
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("sessionId",  row[0]);
                m.put("classId",    row[1]);
                m.put("className",  row[2]);
                m.put("present",    row[3] != null ? row[3] : 0);
                m.put("absent",     row[4] != null ? row[4] : 0);
                m.put("late",       row[5] != null ? row[5] : 0);
                m.put("total",      row[6] != null ? row[6] : 0);
                result.add(m);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to get today attendance for userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * Get students at risk of being banned from exams (attendance < threshold)
     * threshold: students with absence rate > 25%
     */
    public List<Map<String, Object>> getStudentsAtRisk(Integer userId, double absentThreshold) {
        String jpql = """
            SELECT s.fullName, s.email, c.name as className,
                   COUNT(ar) as totalSessions,
                   SUM(CASE WHEN ar.status = 'ABSENT' THEN 1 ELSE 0 END) as absentCount
            FROM AttendanceRecord ar
            JOIN ar.student s
            JOIN ar.session sess
            JOIN sess.classEntity c
            JOIN c.classTeachers ct
            JOIN ct.teacher t
            WHERE t.userId = :userId
              AND (ct.endDate IS NULL OR ct.endDate >= CURRENT_DATE)
              AND ar.deleted = false
              AND c.deletedAt IS NULL
            GROUP BY s.studentId, s.fullName, s.email, c.classId, c.name
            """;
        try {
            List<Object[]> rows = entityManager.createQuery(jpql)
                    .setParameter("userId", userId)
                    .getResultList();
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (Object[] row : rows) {
                long total = ((Number) row[3]).longValue();
                long absent = ((Number) row[4]).longValue();
                double rate = total > 0 ? (absent * 100.0 / total) : 0;
                if (rate >= absentThreshold) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("studentName",   row[0]);
                    m.put("email",         row[1]);
                    m.put("className",     row[2]);
                    m.put("totalSessions", total);
                    m.put("absentCount",   absent);
                    m.put("absentRate",    Math.round(rate * 10.0) / 10.0);
                    result.add(m);
                }
            }
            // Sort by absentRate descending
            result.sort((a, b) -> Double.compare((Double) b.get("absentRate"), (Double) a.get("absentRate")));
            return result;
        } catch (Exception e) {
            log.error("Failed to get at-risk students for userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * Get attendance summary per class for a lecturer
     */
    public List<Map<String, Object>> getLecturerAttendanceSummary(Integer userId) {
        String jpql = """
            SELECT c.classId, c.name,
                   COUNT(DISTINCT s.sessionId) as totalSessions,
                   COUNT(ar) as totalRecords,
                   SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END) as presentCount,
                   SUM(CASE WHEN ar.status = 'ABSENT'  THEN 1 ELSE 0 END) as absentCount,
                   (SELECT COUNT(DISTINCT e.enrollmentId) FROM Enrollment e
                    WHERE e.classEntity.classId = c.classId AND e.status = 'ACTIVE' AND e.revokedAt IS NULL) as enrolledStudents
            FROM ClassTeacher ct
            JOIN ct.classEntity c
            JOIN ct.teacher t
            LEFT JOIN AttendanceSession s ON s.classEntity.classId = c.classId AND s.deleted = false
            LEFT JOIN AttendanceRecord ar ON ar.session = s AND ar.deleted = false
            WHERE t.userId = :userId
              AND (ct.endDate IS NULL OR ct.endDate >= CURRENT_DATE)
              AND c.deletedAt IS NULL
            GROUP BY c.classId, c.name
            ORDER BY c.name
            """;
        try {
            List<Object[]> rows = entityManager.createQuery(jpql)
                    .setParameter("userId", userId)
                    .getResultList();
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("classId",          row[0]);
                m.put("className",        row[1]);
                m.put("totalSessions",    row[2] != null ? row[2] : 0);
                m.put("totalRecords",     row[3] != null ? row[3] : 0);
                m.put("presentCount",     row[4] != null ? row[4] : 0);
                m.put("absentCount",      row[5] != null ? row[5] : 0);
                m.put("enrolledStudents", row[6] != null ? row[6] : 0);
                result.add(m);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to get attendance summary for userId={}", userId, e);
            return List.of();
        }
    }

    // ============================================================
    // ADMIN / ACADEMIC STAFF STATS
    // ============================================================

    /**
     * Get system-wide class status breakdown for Admin
     */
    public List<Map<String, Object>> getAdminClassStatusSummary() {
        String jpql = """
            SELECT c.status, COUNT(c)
            FROM ClassEntity c
            WHERE c.deletedAt IS NULL
            GROUP BY c.status
            """;
        try {
            List<Object[]> rows = entityManager.createQuery(jpql).getResultList();
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("status", row[0] != null ? row[0].toString() : "UNKNOWN");
                m.put("count", row[1]);
                result.add(m);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to get admin class status summary", e);
            return List.of();
        }
    }

    /**
     * Get today's system-wide attendance summary
     */
    public Map<String, Object> getAdminTodayAttendanceSummary() {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String jpql = """
            SELECT COUNT(DISTINCT s.sessionId),
                   SUM(CASE WHEN ar.status = 'PRESENT' THEN 1 ELSE 0 END),
                   SUM(CASE WHEN ar.status = 'ABSENT'  THEN 1 ELSE 0 END),
                   SUM(CASE WHEN ar.status = 'LATE'    THEN 1 ELSE 0 END),
                   COUNT(ar)
            FROM AttendanceSession s
            LEFT JOIN AttendanceRecord ar ON ar.session = s AND ar.deleted = false
            WHERE s.attendanceDate = :today AND s.deleted = false
            """;
        try {
            Object[] row = (Object[]) entityManager.createQuery(jpql)
                    .setParameter("today", today)
                    .getSingleResult();
            Map<String, Object> m = new HashMap<>();
            m.put("today", today.toString());
            m.put("totalSessions", row[0] != null ? row[0] : 0);
            m.put("presentCount",  row[1] != null ? row[1] : 0);
            m.put("absentCount",   row[2] != null ? row[2] : 0);
            m.put("lateCount",     row[3] != null ? row[3] : 0);
            m.put("totalRecords",  row[4] != null ? row[4] : 0);
            return m;
        } catch (Exception e) {
            log.error("Failed to get admin today attendance summary", e);
            return Map.of();
        }
    }

    /**
     * Get classes with highest absence rates for Admin
     */
    public List<Map<String, Object>> getClassesWithHighestAbsence() {
        String jpql = """
            SELECT c.classId, c.name,
                   COUNT(ar) as totalRecords,
                   SUM(CASE WHEN ar.status = 'ABSENT' THEN 1 ELSE 0 END) as absentCount
            FROM ClassEntity c
            JOIN AttendanceSession s ON s.classEntity = c AND s.deleted = false
            JOIN AttendanceRecord ar ON ar.session = s AND ar.deleted = false
            WHERE c.deletedAt IS NULL
            GROUP BY c.classId, c.name
            HAVING COUNT(ar) > 0
            """;
        try {
            List<Object[]> rows = entityManager.createQuery(jpql).getResultList();
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (Object[] row : rows) {
                long total = ((Number) row[2]).longValue();
                long absent = ((Number) row[3]).longValue();
                double rate = total > 0 ? (absent * 100.0 / total) : 0;
                Map<String, Object> m = new HashMap<>();
                m.put("classId", row[0]);
                m.put("className", row[1]);
                m.put("totalRecords", total);
                m.put("absentCount", absent);
                m.put("absentRate", Math.round(rate * 10.0) / 10.0);
                result.add(m);
            }
            result.sort((a, b) -> Double.compare((Double) b.get("absentRate"), (Double) a.get("absentRate")));
            return result.stream().limit(5).toList();
        } catch (Exception e) {
            log.error("Failed to get classes with highest absence", e);
            return List.of();
        }
    }

    /**
     * Get enrollment counts grouped by Program for Admin
     */
    public List<Map<String, Object>> getProgramEnrollmentSummary() {
        String jpql = """
            SELECT p.name, COUNT(DISTINCT e.enrollmentId)
            FROM Program p
            LEFT JOIN ClassEntity c ON c.program = p AND c.deletedAt IS NULL
            LEFT JOIN Enrollment e ON e.classEntity = c AND e.status = 'ACTIVE' AND e.revokedAt IS NULL
            WHERE p.deletedAt IS NULL
            GROUP BY p.programId, p.name
            ORDER BY COUNT(DISTINCT e.enrollmentId) DESC
            """;
        try {
            List<Object[]> rows = entityManager.createQuery(jpql).getResultList();
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("programName", row[0]);
                m.put("enrolledCount", row[1]);
                result.add(m);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to get program enrollment summary", e);
            return List.of();
        }
    }

    /**
     * Get active/planned classes that do NOT have assigned lecturers
     */
    public List<Map<String, Object>> getClassesWithoutLecturer() {
        String jpql = """
            SELECT c.classId, c.name, c.status, p.name as programName
            FROM ClassEntity c
            JOIN c.program p
            WHERE c.deletedAt IS NULL
              AND c.status IN (com.example.sis.models.ClassEntity.ClassStatus.PLANNED, com.example.sis.models.ClassEntity.ClassStatus.ONGOING)
              AND NOT EXISTS (
                  SELECT 1 FROM ClassTeacher ct
                  WHERE ct.classEntity = c AND (ct.endDate IS NULL OR ct.endDate >= CURRENT_DATE)
              )
            ORDER BY c.startDate ASC
            """;
        try {
            List<Object[]> rows = entityManager.createQuery(jpql).getResultList();
            List<Map<String, Object>> result = new java.util.ArrayList<>();
            for (Object[] row : rows) {
                Map<String, Object> m = new HashMap<>();
                m.put("classId", row[0]);
                m.put("className", row[1]);
                m.put("status", row[2] != null ? row[2].toString() : "N/A");
                m.put("programName", row[3]);
                result.add(m);
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to get classes without lecturer", e);
            return List.of();
        }
    }
}
