package com.example.sis.controllers;

import com.example.sis.dto.DashboardSummaryDTO;
import com.example.sis.services.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary(@RequestParam(required = false) Integer centerId) {
        return ResponseEntity.ok(dashboardService.getSummary(centerId));
    }
}
