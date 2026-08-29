package com.example.sis.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardSummaryDTO {
    private long totalCenters;
    private long totalStudents;
    private long totalClasses;
    private long activeCourses; 
    private long totalLecturers; 
}
