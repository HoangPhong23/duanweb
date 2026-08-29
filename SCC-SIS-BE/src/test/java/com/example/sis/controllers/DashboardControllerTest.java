package com.example.sis.controllers;

import com.example.sis.dto.DashboardSummaryDTO;
import com.example.sis.services.DashboardService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest {

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    @Test
    @DisplayName("Should return dashboard summary")
    void shouldReturnDashboardSummary() {
        // GIVEN
        DashboardSummaryDTO mockSummary = DashboardSummaryDTO.builder()
                .totalCenters(1)
                .totalStudents(10)
                .totalClasses(5)
                .activeCourses(2)
                .totalLecturers(3)
                .build();
        
        when(dashboardService.getSummary(null)).thenReturn(mockSummary);

        // WHEN
        ResponseEntity<DashboardSummaryDTO> response = dashboardController.getSummary(null);

        // THEN
        assertNotNull(response);
        assertEquals(200, response.getStatusCodeValue());
        assertEquals(mockSummary, response.getBody());
    }
}
