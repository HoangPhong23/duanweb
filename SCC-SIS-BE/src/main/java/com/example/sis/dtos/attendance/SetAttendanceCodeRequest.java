package com.example.sis.dtos.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO để giảng viên set mã điểm danh cho một buổi
 */
public class SetAttendanceCodeRequest {

    @NotBlank(message = "Mã điểm danh không được để trống")
    @Size(min = 3, max = 20, message = "Mã điểm danh phải từ 3 đến 20 ký tự")
    private String attendanceCode;

    /** true = bật chế độ điểm danh bằng mã, false = tắt */
    @NotNull(message = "codeEnabled là bắt buộc")
    private Boolean codeEnabled;

    /** Số phút mã có hiệu lực (null = không giới hạn) */
    private Integer expiresMinutes;

    public String getAttendanceCode() {
        return attendanceCode;
    }

    public void setAttendanceCode(String attendanceCode) {
        this.attendanceCode = attendanceCode != null ? attendanceCode.trim().toUpperCase() : null;
    }

    public Boolean getCodeEnabled() {
        return codeEnabled;
    }

    public void setCodeEnabled(Boolean codeEnabled) {
        this.codeEnabled = codeEnabled;
    }

    public Integer getExpiresMinutes() {
        return expiresMinutes;
    }

    public void setExpiresMinutes(Integer expiresMinutes) {
        this.expiresMinutes = expiresMinutes;
    }
}
