package com.ranjith.phcbackend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranjith.phcbackend.service.DashboardService;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // ✅ SUMMARY API
    @GetMapping("/summary/{divisionId}")
    public ResponseEntity<?> getSummary(@PathVariable Long divisionId) {

        Map<String, Object> summary =
                dashboardService.getSummary(divisionId);

        return ResponseEntity.ok(summary);
    }
}