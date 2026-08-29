package com.example.sis.repositories.projections;

public interface DashboardSummaryProjection {
    Long getTotalCenters();
    Long getTotalStudents();
    Long getTotalClasses();
    Long getActiveCourses();
    Long getTotalLecturers();
}
