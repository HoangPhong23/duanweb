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

    // Endpoint: GET /api/v1/dashboard/summary
    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary(@RequestParam(name = "centerId", required = false) String centerIdParam) {
        Integer centerId = null;
        if (centerIdParam != null && !centerIdParam.trim().isEmpty() && !centerIdParam.equalsIgnoreCase("null") && !centerIdParam.equalsIgnoreCase("undefined") && !centerIdParam.equalsIgnoreCase("all")) {
            try {
                centerId = Integer.parseInt(centerIdParam.trim());
            } catch (NumberFormatException e) {
                // Ignore invalid numbers and treat as null (all centers)
            }
        }
        return ResponseEntity.ok(dashboardService.getSummary(centerId));
    }
}
