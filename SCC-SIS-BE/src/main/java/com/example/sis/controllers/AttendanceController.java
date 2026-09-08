package com.example.sis.controllers;

import com.example.sis.dtos.attendance.*;
import com.example.sis.exceptions.BadRequestException;
import com.example.sis.exceptions.NotFoundException;
import com.example.sis.repositories.UserRoleRepository;
import com.example.sis.services.AttendanceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserRoleRepository userRoleRepository;
    private final com.example.sis.services.ClassAttendanceStatisticsService classAttendanceStatisticsService;

    public AttendanceController(AttendanceService attendanceService,
                               UserRoleRepository userRoleRepository,
                               com.example.sis.services.ClassAttendanceStatisticsService classAttendanceStatisticsService) {
        this.attendanceService = attendanceService;
        this.userRoleRepository = userRoleRepository;
        this.classAttendanceStatisticsService = classAttendanceStatisticsService;
    }

    /**
     * GET /api/attendance-schedules?teacher_id={teacher_id}&from={yyyy-mm-dd}&to={yyyy-mm-dd}
     * Lấy lịch dạy của giảng viên
     */
    @GetMapping("/attendance-schedules")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<List<TeacherScheduleResponse>> getTeacherSchedule(
            @RequestParam Integer teacher_id,
            @RequestParam String from,
            @RequestParam String to,
            Authentication authentication) {

        try {
            if (!isCurrentUserSuperAdmin(authentication)) {
                Integer currentUserId = getCurrentUserId(authentication);
                if (currentUserId == null || !currentUserId.equals(teacher_id)) {
                    return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
                }
            }

            LocalDate fromDate = LocalDate.parse(from);
            LocalDate toDate = LocalDate.parse(to);

            List<TeacherScheduleResponse> schedules = attendanceService.getTeacherSchedule(teacher_id, fromDate, toDate);
            return ResponseEntity.ok(schedules);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * POST /api/attendance-sessions
     * Tạo buổi điểm danh mới
     */
    @PostMapping("/attendance-sessions")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<AttendanceSessionResponse> createAttendanceSession(
            @Valid @RequestBody CreateAttendanceSessionRequest request,
            Authentication authentication) {

        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AttendanceSessionResponse response = attendanceService.createSession(request, currentUserId);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BadRequestException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * GET /api/classes/{class_id}/attendance-sessions
     * Lấy danh sách buổi điểm danh của một lớp
     */
    @GetMapping("/classes/{class_id}/attendance-sessions")
    public ResponseEntity<List<AttendanceSessionSummaryResponse>> getAttendanceSessionsByClass(
            @PathVariable("class_id") Integer classId) {
        List<AttendanceSessionSummaryResponse> sessions = attendanceService.getSessionsByClass(classId);
        return ResponseEntity.ok(sessions);
    }

    /**
     * GET /api/attendance-sessions/{session_id}
     * Lấy chi tiết một buổi điểm danh
     */
    @GetMapping("/attendance-sessions/{session_id}")
    public ResponseEntity<AttendanceSessionResponse> getAttendanceSessionDetail(
            @PathVariable("session_id") Integer sessionId) {
        try {
            AttendanceSessionResponse response = attendanceService.getSessionDetail(sessionId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * PUT /api/attendance-sessions/{session_id}
     * Cập nhật buổi điểm danh
     */
    @PutMapping("/attendance-sessions/{session_id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<AttendanceSessionResponse> updateAttendanceSession(
            @PathVariable("session_id") Integer sessionId,
            @Valid @RequestBody UpdateAttendanceSessionRequest request,
            Authentication authentication) {

        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            AttendanceSessionResponse response = attendanceService.updateSession(sessionId, request, currentUserId);
            return ResponseEntity.ok(response);
        } catch (BadRequestException | NotFoundException e) {
            throw e;
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * DELETE /api/attendance-sessions/{session_id}
     * Xóa buổi điểm danh (soft delete)
     */
    @DeleteMapping("/attendance-sessions/{session_id}")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<Void> deleteAttendanceSession(
            @PathVariable("session_id") Integer sessionId,
            Authentication authentication) {

        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            attendanceService.deleteSession(sessionId, currentUserId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * GET /api/attendance/my-attendance/{classId}
     * Lấy lịch sử điểm danh của học viên hiện tại (dựa vào token)
     */
    @GetMapping("/attendance/my-attendance/{classId}")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<StudentAttendanceHistoryResponse> getMyAttendanceByClass(
            @PathVariable Integer classId,
            Authentication authentication) {
        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            StudentAttendanceHistoryResponse response = attendanceService.getMyAttendanceByClass(currentUserId, classId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.println("Error fetching my attendance: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * GET /api/students/{student_id}/classes/{class_id}/attendance
     * Lấy lịch sử điểm danh của học viên trong một lớp
     */
    @GetMapping("/students/{student_id}/classes/{class_id}/attendance")
    public ResponseEntity<StudentAttendanceHistoryResponse> getStudentAttendanceHistory(
            @PathVariable("student_id") Integer studentId,
            @PathVariable("class_id") Integer classId) {
        try {
            StudentAttendanceHistoryResponse response = attendanceService.getStudentAttendanceHistory(studentId, classId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            e.printStackTrace();
            System.err.println("Error fetching student attendance history: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
    }

    /**
     * GET /api/classes/{classId}/attendance/statistics?month={month}&year={year}
     * Lấy thống kê điểm danh của lớp học theo tháng/năm
     */
    @GetMapping("/classes/{classId}/attendance/statistics")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or " +
                  "@authz.hasRole(authentication, 'LECTURER') or " +
                  "@authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<ClassAttendanceStatisticsDTO> getClassAttendanceStatistics(
            @PathVariable Integer classId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            Authentication authentication) {

        try {
            if (month < 1 || month > 12) return ResponseEntity.badRequest().build();
            if (year < 2000 || year > 2100) return ResponseEntity.badRequest().build();

            ClassAttendanceStatisticsDTO statistics = classAttendanceStatisticsService
                    .getClassAttendanceStatistics(classId, month, year);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            System.err.println("Error fetching class attendance statistics: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/classes/{classId}/attendance/export/excel?month={month}&year={year}
     * Xuất thống kê điểm danh ra file Excel
     */
    @GetMapping("/classes/{classId}/attendance/export/excel")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or " +
                  "@authz.hasRole(authentication, 'LECTURER') or " +
                  "@authz.hasRole(authentication, 'ACADEMIC_STAFF')")
    public ResponseEntity<byte[]> exportAttendanceToExcel(
            @PathVariable Integer classId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            Authentication authentication) {

        try {
            if (month < 1 || month > 12) return ResponseEntity.badRequest().build();
            if (year < 2000 || year > 2100) return ResponseEntity.badRequest().build();

            byte[] excelFile = classAttendanceStatisticsService.exportToExcel(classId, month, year);
            String filename = String.format("attendance_statistics_class%d_%d_%d.xlsx", classId, month, year);

            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                    .body(excelFile);
        } catch (Exception e) {
            System.err.println("Error exporting attendance to Excel: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===================================================================
    //  ĐIỂM DANH BẰNG MÃ
    // ===================================================================

    /**
     * POST /api/attendance-sessions/{sessionId}/code
     * Giảng viên set/bật/tắt mã điểm danh cho một buổi đã tạo
     */
    @PostMapping("/attendance-sessions/{sessionId}/code")
    @PreAuthorize("@authz.isSuperAdmin(authentication) or @authz.hasRole(authentication, 'LECTURER')")
    public ResponseEntity<SetAttendanceCodeResponse> setAttendanceCode(
            @PathVariable("sessionId") Integer sessionId,
            @Valid @RequestBody SetAttendanceCodeRequest request,
            Authentication authentication) {

        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            SetAttendanceCodeResponse response = attendanceService.setAttendanceCode(sessionId, request, currentUserId);
            return ResponseEntity.ok(response);
        } catch (com.example.sis.exceptions.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        } catch (Exception e) {
            System.err.println("Error setting attendance code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    /**
     * POST /api/attendance/submit-code
     * Học viên submit mã để tự điểm danh PRESENT
     */
    @PostMapping("/attendance/submit-code")
    @PreAuthorize("@authz.hasRole(authentication, 'STUDENT')")
    public ResponseEntity<Void> submitAttendanceCode(
            @Valid @RequestBody StudentSubmitCodeRequest request,
            Authentication authentication) {

        Integer currentUserId = getCurrentUserId(authentication);
        if (currentUserId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            attendanceService.submitAttendanceCode(request, currentUserId);
            return ResponseEntity.ok().build();
        } catch (com.example.sis.exceptions.BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .header("X-Error-Message", e.getMessage())
                    .build();
        } catch (com.example.sis.exceptions.NotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .header("X-Error-Message", e.getMessage())
                    .build();
        } catch (Exception e) {
            System.err.println("Error submitting attendance code: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // ===== Helpers =====

    private Integer getCurrentUserId(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String keycloakUserId = jwt.getSubject();
            return userRoleRepository.findUserIdByKeycloakUserId(keycloakUserId);
        }
        return null;
    }

    private boolean isCurrentUserSuperAdmin(Authentication authentication) {
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String sub = jwt.getClaimAsString("sub");
            return userRoleRepository.userHasActiveRoleByKeycloakIdAndRoleCode(sub, "SUPER_ADMIN");
        }
        return false;
    }
}
