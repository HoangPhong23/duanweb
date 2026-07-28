package com.example.sis.dtos.attendance;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO để học viên submit mã điểm danh
 */
public class StudentSubmitCodeRequest {

    @NotNull(message = "classId là bắt buộc")
    private Integer classId;

    @NotBlank(message = "Mã điểm danh không được để trống")
    private String attendanceCode;

    public Integer getClassId() {
        return classId;
    }

    public void setClassId(Integer classId) {
        this.classId = classId;
    }

    public String getAttendanceCode() {
        return attendanceCode;
    }

    public void setAttendanceCode(String attendanceCode) {
        this.attendanceCode = attendanceCode != null ? attendanceCode.trim().toUpperCase() : null;
    }
}
