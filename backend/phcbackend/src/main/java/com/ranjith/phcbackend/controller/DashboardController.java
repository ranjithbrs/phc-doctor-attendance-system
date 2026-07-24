package com.ranjith.phcbackend.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranjith.phcbackend.service.DashboardService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // ✅ SUMMARY
    @GetMapping("/summary/{divisionId}")
    public Map<String, Object> getSummary(@PathVariable Long divisionId) {
        return dashboardService.getSummary(divisionId);
    }

    // ✅ TABLE
    @GetMapping("/phc-overview/{divisionId}")
    public List<Map<String, Object>> getPhcOverview(@PathVariable Long divisionId) {
        return dashboardService.getPhcOverview(divisionId);
    }
}