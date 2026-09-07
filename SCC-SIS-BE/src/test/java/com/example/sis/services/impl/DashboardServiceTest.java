package com.example.sis.services.impl;

import com.example.sis.dto.DashboardSummaryDTO;
import com.example.sis.repositories.DashboardRepository;
import com.example.sis.repositories.projections.DashboardSummaryProjection;
import com.example.sis.services.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Unit Tests")
class DashboardServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @InjectMocks
    private DashboardService dashboardService;

    @Test
    @DisplayName("Should return summary dto from projection")
    void shouldReturnSummaryDto() {
        // GIVEN
        DashboardSummaryProjection mockProj = new DashboardSummaryProjection() {
            @Override public Long getTotalCenters() { return 1L; }
            @Override public Long getTotalStudents() { return 10L; }
            @Override public Long getTotalClasses() { return 5L; }
            @Override public Long getActiveCourses() { return 2L; }
            @Override public Long getTotalLecturers() { return 3L; }
        };

        when(dashboardRepository.getDashboardSummaryNativeByCenter(1)).thenReturn(mockProj);

        // WHEN
        DashboardSummaryDTO result = dashboardService.getSummary(1);

        // THEN
        assertNotNull(result);
        assertEquals(1L, result.getTotalCenters());
        assertEquals(10L, result.getTotalStudents());
        assertEquals(5L, result.getTotalClasses());
        assertEquals(2L, result.getActiveCourses());
        assertEquals(3L, result.getTotalLecturers());
    }
}
