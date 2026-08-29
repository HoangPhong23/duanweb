package com.example.sis.repositories;

import com.example.sis.models.Center;
import com.example.sis.repositories.projections.DashboardSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DashboardRepository extends JpaRepository<Center, Integer> {
    
    @Query(value = "SELECT " +
        "(SELECT COUNT(*) FROM centers) AS totalCenters, " +
        "(SELECT COUNT(DISTINCT e.student_id) FROM enrollments e " +
        " LEFT JOIN classes c ON e.class_id = c.id " +
        " WHERE e.status IN ('ACTIVE', 'PENDING') " +
        " AND (:centerId IS NULL OR c.center_id = :centerId)) AS totalStudents, " +
        "(SELECT COUNT(*) FROM classes c WHERE :centerId IS NULL OR c.center_id = :centerId) AS totalClasses, " +
        "(SELECT COUNT(*) FROM programs) AS activeCourses, " +
        "(SELECT COUNT(DISTINCT ur.user_id) FROM user_roles ur " +
        " JOIN roles r ON ur.role_id = r.id " +
        " WHERE r.code = 'LECTURER' " +
        " AND (:centerId IS NULL OR ur.center_id = :centerId)) AS totalLecturers", 
       nativeQuery = true)
    DashboardSummaryProjection getDashboardSummaryNative(@Param("centerId") Integer centerId);
}
