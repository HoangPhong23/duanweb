package com.example.sis.service.chat;

import com.example.sis.service.system.SystemStatsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service to detect when user asks questions about real-time system data
 * and fetch that data from internal APIs.
 * Supports both Student and Lecturer role queries.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RealtimeDataFetcher {

    private final SystemStatsService systemStatsService;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${server.port:7000}")
    private int serverPort;

    /**
     * Analyze user message and determine if it requires real-time data
     */
    public boolean needsRealtimeData(String userMessage) {
        String msg = userMessage.toLowerCase();
        return msg.contains("bao nhiêu") ||
               msg.contains("thống kê") ||
               msg.contains("hiện tại") ||
               msg.contains("hôm nay") ||
               msg.contains("bao giờ") ||
               msg.contains("khi nào") ||
               msg.contains("còn") ||
               msg.contains("đang có") ||
               msg.contains("tổng số") ||
               msg.contains("số lượng") ||
               msg.contains("điểm danh") ||
               msg.contains("vắng mặt") ||
               msg.contains("cấm thi") ||
               msg.contains("nguy cơ") ||
               msg.contains("lớp của tôi") ||
               msg.contains("lớp tôi") ||
               msg.contains("chuyên cần") ||
               msg.contains("danh sách") ||
               msg.contains("học viên") ||
               // ── Lecturer keywords ──
               msg.contains("đang dạy") ||
               msg.contains("lớp nào") ||
               msg.contains("được giao") ||
               msg.contains("phân công") ||
               msg.contains("lớp học") ||
               msg.contains("báo cáo") ||
               msg.contains("tổng quan") ||
               msg.contains("nghỉ nhiều") ||
               msg.contains("vắng nhiều") ||
               // ── Admin keywords ──
               msg.contains("tình trạng") ||
               msg.contains("trạng thái") ||
               msg.contains("hoạt động") ||
               msg.contains("ghi danh") ||
               msg.contains("chương trình") ||
               msg.contains("tỷ lệ vắng") ||
               msg.contains("vắng cao") ||
               msg.contains("giảng viên") ||
               msg.contains("chưa phân công") ||
               msg.contains("thiếu giảng viên");
    }

    /**
     * Overload without userContext (backward compat)
     */
    public String buildContextWithRealtimeData(String userMessage, String baseContext) {
        return buildContextWithRealtimeData(userMessage, baseContext, null);
    }

    /**
     * Main entry: fetch real-time data based on user question and role context.
     */
    public String buildContextWithRealtimeData(String userMessage, String baseContext, Map<String, Object> userContext) {
        log.info("🔍 RealtimeDataFetcher called with message: {}", userMessage);

        if (!needsRealtimeData(userMessage)) {
            log.info("❌ No real-time data keywords detected, using RAG only");
            return baseContext;
        }

        log.info("✅ Real-time data keywords detected!");

        StringBuilder dataContext = new StringBuilder();
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        String msg = userMessage.toLowerCase();
        boolean hasRealtimeData = false;

        boolean isLecturer = userContext != null && Boolean.TRUE.equals(userContext.get("isLecturer"));

        // ═══════════════════════════════════════════════════
        // LECTURER QUERIES
        // ═══════════════════════════════════════════════════
        if (isLecturer) {
            Integer lecturerUserId = userContext != null ? (Integer) userContext.get("lecturerUserId") : null;

            if (lecturerUserId != null) {

                // 1. Danh sách lớp được phân công
                if (msg.contains("lớp của tôi") || msg.contains("lớp tôi") ||
                    msg.contains("lớp nào") || msg.contains("đang dạy") ||
                    msg.contains("được gán") || msg.contains("phân công")) {
                    try {
                        List<Map<String, Object>> classes = systemStatsService.getLecturerClasses(lecturerUserId);
                        if (!hasRealtimeData) {
                            dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                            dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                            hasRealtimeData = true;
                        }
                        dataContext.append(String.format("DANH SÁCH LỚP ĐANG PHỤ TRÁCH (%d lớp):\n", classes.size()));
                        if (classes.isEmpty()) {
                            dataContext.append("- Hiện tại chưa được phân công lớp nào.\n");
                        } else {
                            for (Map<String, Object> c : classes) {
                                dataContext.append(String.format(
                                    "  • %s | Chương trình: %s | Trạng thái: %s | Phòng: %s | Ca: %s\n",
                                    c.get("className"), c.get("programName"), c.get("status"),
                                    c.get("room"), c.get("studyTime")
                                ));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to get lecturer classes for userId={}", lecturerUserId, e);
                    }
                }

                // 2. Điểm danh hôm nay
                if (msg.contains("điểm danh") || msg.contains("hôm nay") ||
                    msg.contains("vắng") || msg.contains("có mặt")) {
                    try {
                        List<Map<String, Object>> sessions = systemStatsService.getLecturerTodayAttendance(lecturerUserId);
                        if (!hasRealtimeData) {
                            dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                            dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                            hasRealtimeData = true;
                        }
                        if (sessions.isEmpty()) {
                            dataContext.append(String.format(
                                "ĐIỂM DANH HÔM NAY (%s): Không có buổi điểm danh nào hôm nay.\n", today));
                        } else {
                            dataContext.append(String.format("ĐIỂM DANH HÔM NAY (%s):\n", today));
                            for (Map<String, Object> s : sessions) {
                                dataContext.append(String.format(
                                    "  • Lớp %s: Có mặt %s | Vắng %s | Muộn %s | Tổng %s học viên\n",
                                    s.get("className"), s.get("present"), s.get("absent"),
                                    s.get("late"), s.get("total")
                                ));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to get today attendance for userId={}", lecturerUserId, e);
                    }
                }

                // 3. Học viên nguy cơ cấm thi
                if (msg.contains("cấm thi") || msg.contains("nguy cơ") ||
                    msg.contains("nghỉ nhiều") || msg.contains("vắng nhiều")) {
                    try {
                        List<Map<String, Object>> atRisk = systemStatsService.getStudentsAtRisk(lecturerUserId, 25.0);
                        if (!hasRealtimeData) {
                            dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                            dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                            hasRealtimeData = true;
                        }
                        if (atRisk.isEmpty()) {
                            dataContext.append("HỌC VIÊN NGUY CƠ CẤM THI: Không có học viên nào vắng > 25%.\n");
                        } else {
                            dataContext.append(String.format(
                                "HỌC VIÊN NGUY CƠ CẤM THI (vắng > 25%%) — Tổng: %d người:\n", atRisk.size()));
                            for (Map<String, Object> sv : atRisk) {
                                dataContext.append(String.format(
                                    "  ⚠️ %s (%s) | Lớp: %s | Vắng: %s/%s buổi (%.0f%%)\n",
                                    sv.get("studentName"), sv.get("email"), sv.get("className"),
                                    sv.get("absentCount"), sv.get("totalSessions"),
                                    ((Number) sv.get("absentRate")).doubleValue()
                                ));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to get at-risk students for userId={}", lecturerUserId, e);
                    }
                }

                // 4. Thống kê chuyên cần tổng quan
                if (msg.contains("thống kê") || msg.contains("chuyên cần") ||
                    msg.contains("tổng quan") || msg.contains("báo cáo")) {
                    try {
                        List<Map<String, Object>> summary = systemStatsService.getLecturerAttendanceSummary(lecturerUserId);
                        if (!hasRealtimeData) {
                            dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                            dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                            hasRealtimeData = true;
                        }
                        dataContext.append("THỐNG KÊ CHUYÊN CẦN THEO LỚP:\n");
                        if (summary.isEmpty()) {
                            dataContext.append("- Chưa có dữ liệu điểm danh.\n");
                        } else {
                            for (Map<String, Object> cls : summary) {
                                long total   = ((Number) cls.get("totalRecords")).longValue();
                                long present = ((Number) cls.get("presentCount")).longValue();
                                double rate  = total > 0 ? (present * 100.0 / total) : 0;
                                dataContext.append(String.format(
                                    "  • %s: %d học viên | %d buổi | Có mặt: %d | Vắng: %d | Chuyên cần: %.1f%%\n",
                                    cls.get("className"),
                                    ((Number) cls.get("enrolledStudents")).longValue(),
                                    ((Number) cls.get("totalSessions")).longValue(),
                                    present,
                                    ((Number) cls.get("absentCount")).longValue(),
                                    rate
                                ));
                            }
                        }
                    } catch (Exception e) {
                        log.error("Failed to get attendance summary for userId={}", lecturerUserId, e);
                    }
                }
            }
        }
        // ═══════════════════════════════════════════════════
        // ADMIN / ACADEMIC STAFF QUERIES
        // ═══════════════════════════════════════════════════
        boolean isAdmin = userContext != null && Boolean.TRUE.equals(userContext.get("isAdmin"));
        if (isAdmin) {
            // 1. Tình trạng các lớp học đang hoạt động / Phân bố trạng thái lớp
            if (msg.contains("tình trạng") || msg.contains("trạng thái lớp") || msg.contains("lớp học đang hoạt động") || msg.contains("hoạt động")) {
                try {
                    List<Map<String, Object>> statusList = systemStatsService.getAdminClassStatusSummary();
                    if (!hasRealtimeData) {
                        dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                        dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                        hasRealtimeData = true;
                    }
                    dataContext.append("PHÂN BỐ TRẠNG THÁI LỚP HỌC TOÀN HỆ THỐNG:\n");
                    for (Map<String, Object> st : statusList) {
                        dataContext.append(String.format("  • Trạng thái %s: %s lớp\n", st.get("status"), st.get("count")));
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch admin class status summary", e);
                }
            }

            // 2. Thống kê điểm danh toàn bộ hôm nay
            if (msg.contains("điểm danh toàn bộ") || msg.contains("điểm danh toàn trung tâm") || (msg.contains("điểm danh") && msg.contains("hôm nay"))) {
                try {
                    Map<String, Object> todayAtt = systemStatsService.getAdminTodayAttendanceSummary();
                    if (!hasRealtimeData) {
                        dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                        dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                        hasRealtimeData = true;
                    }
                    dataContext.append(String.format("THỐNG KÊ ĐIỂM DANH TOÀN HỆ THỐNG HÔM NAY (%s):\n", today));
                    dataContext.append(String.format("  • Tổng số buổi điểm danh: %s\n", todayAtt.get("totalSessions")));
                    dataContext.append(String.format("  • Có mặt: %s | Vắng: %s | Muộn: %s (Tổng bản ghi: %s)\n",
                            todayAtt.get("presentCount"), todayAtt.get("absentCount"), todayAtt.get("lateCount"), todayAtt.get("totalRecords")));
                } catch (Exception e) {
                    log.error("Failed to fetch admin today attendance summary", e);
                }
            }

            // 3. Lớp có tỷ lệ vắng cao nhất
            if (msg.contains("vắng cao nhất") || msg.contains("tỷ lệ vắng") || msg.contains("nghỉ nhiều nhất")) {
                try {
                    List<Map<String, Object>> highestAbsence = systemStatsService.getClassesWithHighestAbsence();
                    if (!hasRealtimeData) {
                        dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                        dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                        hasRealtimeData = true;
                    }
                    dataContext.append("TOP 5 LỚP CÓ TỶ LỆ VẮNG HIGHEST:\n");
                    if (highestAbsence.isEmpty()) {
                        dataContext.append("- Chưa có dữ liệu vắng mặt.\n");
                    } else {
                        for (Map<String, Object> c : highestAbsence) {
                            dataContext.append(String.format("  ⚠️ Lớp %s: Tỷ lệ vắng %.1f%% (%s/%s lượt)\n",
                                    c.get("className"), ((Number) c.get("absentRate")).doubleValue(), c.get("absentCount"), c.get("totalRecords")));
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch classes with highest absence", e);
                }
            }

            // 4. Thống kê ghi danh theo chương trình
            if (msg.contains("ghi danh") || msg.contains("chương trình") || msg.contains("theo chương trình")) {
                try {
                    List<Map<String, Object>> progSummary = systemStatsService.getProgramEnrollmentSummary();
                    if (!hasRealtimeData) {
                        dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                        dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                        hasRealtimeData = true;
                    }
                    dataContext.append("THỐNG KÊ HỌC VIÊN GHI DANH THEO CHƯƠNG TRÌNH:\n");
                    for (Map<String, Object> p : progSummary) {
                        dataContext.append(String.format("  • %s: %s học viên đang học\n", p.get("programName"), p.get("enrolledCount")));
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch program enrollment summary", e);
                }
            }

            // 5. Lớp chưa có giảng viên phụ trách
            if (msg.contains("chưa có giảng viên") || msg.contains("thiếu giảng viên") || msg.contains("chưa phân công")) {
                try {
                    List<Map<String, Object>> noLecturer = systemStatsService.getClassesWithoutLecturer();
                    if (!hasRealtimeData) {
                        dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                        dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                        hasRealtimeData = true;
                    }
                    dataContext.append(String.format("DANH SÁCH LỚP CHƯA CÓ GIẢNG VIÊN PHỤ TRÁCH (%d lớp):\n", noLecturer.size()));
                    if (noLecturer.isEmpty()) {
                        dataContext.append("  • Tất cả các lớp kế hoạch/đang chạy đều đã có giảng viên phân công.\n");
                    } else {
                        for (Map<String, Object> c : noLecturer) {
                            dataContext.append(String.format("  ⚠️ Lớp %s (Chương trình: %s, Trạng thái: %s)\n",
                                    c.get("className"), c.get("programName"), c.get("status")));
                        }
                    }
                } catch (Exception e) {
                    log.error("Failed to fetch classes without lecturer", e);
                }
            }
        }

        // ═══════════════════════════════════════════════════
        // GENERAL (Student / General) QUERIES
        // ═══════════════════════════════════════════════════
        if (!isLecturer && !isAdmin) {
            try {
                // Current date question
                if ((msg.contains("hôm nay") || msg.contains("ngày hôm nay") || msg.contains("bây giờ")) &&
                    (msg.contains("ngày") || msg.contains("tháng") || msg.contains("năm") || msg.contains("bao nhiêu"))) {
                    if (!hasRealtimeData) {
                        dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                        dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n", today));
                        dataContext.append("⚠️ BẮT BUỘC dùng ngày này, KHÔNG dùng ngày khác!\n\n");
                        hasRealtimeData = true;
                    }
                    dataContext.append(String.format("- Ngày hiện tại: %s (%02d/%02d/%d)\n",
                        today, today.getDayOfMonth(), today.getMonthValue(), today.getYear()));
                }

                // Total users
                if (msg.contains("bao nhiêu user") || msg.contains("bao nhiêu người dùng") ||
                    (msg.contains("user") && msg.contains("bao nhiêu"))) {
                    Long count = systemStatsService.getTotalUsers();
                    if (!hasRealtimeData) {
                        dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                        dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                        hasRealtimeData = true;
                    }
                    dataContext.append(String.format("- Tổng số người dùng: %d người\n", count));
                }

                // Students
                if (msg.contains("học viên") || msg.contains("sinh viên")) {
                    Long totalStudents  = systemStatsService.getTotalStudents();
                    Long activeStudents = systemStatsService.getActiveStudents();
                    if (!hasRealtimeData) {
                        dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                        dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                        hasRealtimeData = true;
                    }
                    dataContext.append(String.format("- Tổng số học viên: %d người\n", totalStudents));
                    dataContext.append(String.format("- Học viên đang hoạt động: %d người\n", activeStudents));
                    dataContext.append(String.format("- Học viên chưa đăng ký lớp: %d người\n", totalStudents - activeStudents));
                }

                // Class start date
                if (msg.contains("lớp") && (msg.contains("bao giờ") || msg.contains("khi nào") ||
                    msg.contains("còn") || msg.contains("bắt đầu") || msg.contains("khai giảng"))) {

                    String cleanedMessage = userMessage.replace("\"", "").replace("'", "")
                        .replaceAll("của\\s+tôi", "").trim();

                    Pattern classPattern = Pattern.compile(
                        "lớp\\s+([\\w\\s\\-ơưăâêôúíóáéýừứửữựăắằẳẵặâầấẩẫậêềếểễệôồốổỗộơờớởỡợưừứửữựđ]+?)(?:\\s+(?:còn|bao|khi|nữa|tính)|$)",
                        Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE
                    );
                    Matcher matcher = classPattern.matcher(cleanedMessage);

                    if (matcher.find()) {
                        String className = matcher.group(1).trim();
                        Map<String, Object> data = systemStatsService.getClassStartInfo(className);
                        if (!hasRealtimeData) {
                            dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                            dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                            hasRealtimeData = true;
                        }
                        if (Boolean.TRUE.equals(data.get("success"))) {
                            dataContext.append("THÔNG TIN LỚP HỌC:\n");
                            dataContext.append(String.format("- Tên lớp: %s\n", data.get("class_name")));
                            dataContext.append(String.format("- Ngày khai giảng: %s\n", data.get("start_date")));
                            dataContext.append(String.format("- SỐ NGÀY CÒN LẠI: %s ngày\n", data.get("days_until_start")));
                            dataContext.append(String.format("- Trạng thái: %s | %s\n", data.get("status"), data.get("message")));
                        } else {
                            dataContext.append(String.format("⚠️ %s\n", data.get("error")));
                        }
                    }
                }

                // System overview
                if (msg.contains("thống kê") || msg.contains("tổng quan")) {
                    Map<String, Object> data = systemStatsService.getSystemStats();
                    if (!hasRealtimeData) {
                        dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME (Ưu tiên trả lời) ===\n");
                        dataContext.append(String.format("📅 NGÀY HÔM NAY: %s\n\n", today));
                        hasRealtimeData = true;
                    }
                    dataContext.append("THỐNG KÊ TỔNG QUAN HỆ THỐNG:\n");
                    dataContext.append(String.format("- Tổng số người dùng: %s\n", data.get("total_users")));
                    dataContext.append(String.format("- Tổng số học viên: %s\n", data.get("total_students")));
                    dataContext.append(String.format("- Học viên đang hoạt động: %s\n", data.get("active_students")));
                    dataContext.append(String.format("- Tổng số giảng viên: %s\n", data.get("total_instructors")));
                    dataContext.append(String.format("- Tổng số quản trị viên: %s\n", data.get("total_admins")));
                }

            } catch (Exception e) {
                log.error("Failed to fetch general realtime data", e);
                if (!hasRealtimeData) {
                    dataContext.append("\n\n=== DỮ LIỆU DATABASE REALTIME ===\n");
                }
                dataContext.append("⚠️ Không thể lấy dữ liệu thời gian thực.\n");
            }
        }

        // Append RAG document context at the end
        if (baseContext != null && !baseContext.isEmpty()) {
            dataContext.append("\n\n=== TÀI LIỆU THAM KHẢO (Chỉ dùng nếu không có dữ liệu realtime) ===\n");
            dataContext.append(baseContext);
        }

        String result = dataContext.toString();
        log.info("📤 Final context length: {} chars, hasRealtimeData: {}", result.length(), hasRealtimeData);
        return result;
    }
}
