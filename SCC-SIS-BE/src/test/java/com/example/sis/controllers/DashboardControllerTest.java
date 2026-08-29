package com.example.sis.controllers;

import com.example.sis.dto.DashboardSummaryDTO;
import com.example.sis.services.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardController Unit Tests")
class DashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private DashboardController dashboardController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(dashboardController).build();
    }

    @Test
    @DisplayName("Should return dashboard summary")
    void shouldReturnDashboardSummary() throws Exception {
        // GIVEN
        DashboardSummaryDTO mockSummary = DashboardSummaryDTO.builder()
                .totalCenters(1L)
                .totalStudents(10L)
                .totalClasses(5L)
                .activeCourses(2L)
                .totalLecturers(3L)
                .build();
        
        when(dashboardService.getSummary(null)).thenReturn(mockSummary);

        // WHEN & THEN
        mockMvc.perform(get("/api/v1/dashboard/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCenters").value(1))
                .andExpect(jsonPath("$.totalStudents").value(10))
                .andExpect(jsonPath("$.totalClasses").value(5))
                .andExpect(jsonPath("$.activeCourses").value(2))
                .andExpect(jsonPath("$.totalLecturers").value(3));
    }
}
