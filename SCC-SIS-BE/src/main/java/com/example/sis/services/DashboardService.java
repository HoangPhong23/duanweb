package com.example.sis.services;

import com.example.sis.dto.DashboardSummaryDTO;
import com.example.sis.repositories.DashboardRepository;
import com.example.sis.repositories.projections.DashboardSummaryProjection;
import org.springframework.stereotype.Service;

@Service
public class DashboardService {
    private final DashboardRepository dashboardRepository;

    public DashboardService(DashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    public DashboardSummaryDTO getSummary(Integer centerId) {
        DashboardSummaryProjection proj = dashboardRepository.getDashboardSummaryNative(centerId);
        
        return DashboardSummaryDTO.builder()
                .totalCenters(proj != null && proj.getTotalCenters() != null ? proj.getTotalCenters() : 0)
                .totalStudents(proj != null && proj.getTotalStudents() != null ? proj.getTotalStudents() : 0)
                .totalClasses(proj != null && proj.getTotalClasses() != null ? proj.getTotalClasses() : 0)
                .activeCourses(proj != null && proj.getActiveCourses() != null ? proj.getActiveCourses() : 0)
                .totalLecturers(proj != null && proj.getTotalLecturers() != null ? proj.getTotalLecturers() : 0)
                .build();
    }
}
