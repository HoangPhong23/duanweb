package com.example.sis.dtos.attendance;

import java.time.LocalDateTime;

/**
 * DTO response trả về thông tin mã điểm danh của một buổi
 */
public class SetAttendanceCodeResponse {

    private Integer sessionId;
    private String attendanceCode;
    private Boolean codeEnabled;
    private LocalDateTime codeExpiresAt;
    private String message;

    public Integer getSessionId() {
        return sessionId;
    }

    public void setSessionId(Integer sessionId) {
        this.sessionId = sessionId;
    }

    public String getAttendanceCode() {
        return attendanceCode;
    }

    public void setAttendanceCode(String attendanceCode) {
        this.attendanceCode = attendanceCode;
    }

    public Boolean getCodeEnabled() {
        return codeEnabled;
    }

    public void setCodeEnabled(Boolean codeEnabled) {
        this.codeEnabled = codeEnabled;
    }

    public LocalDateTime getCodeExpiresAt() {
        return codeExpiresAt;
    }

    public void setCodeExpiresAt(LocalDateTime codeExpiresAt) {
        this.codeExpiresAt = codeExpiresAt;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
