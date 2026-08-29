package com.example.sis.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryDTO {
    private long totalCenters;
    private long totalStudents;
    private long totalClasses;
    private long activeCourses; 
    private long totalLecturers; 
}
