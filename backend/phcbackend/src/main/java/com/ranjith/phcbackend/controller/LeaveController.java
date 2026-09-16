package com.ranjith.phcbackend.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranjith.phcbackend.model.LeaveRequest;
import com.ranjith.phcbackend.service.LeaveService;

@RestController
@RequestMapping("/leave")
@CrossOrigin(origins = "*")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    @PostMapping("/apply")
    public ResponseEntity<?> applyLeave(@RequestBody Map<String, Object> request) {
        if (request == null || request.get("doctorId") == null || request.get("startDate") == null || request.get("endDate") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Doctor ID, startDate, and endDate are required"));
        }

        Long doctorId;
        try {
            doctorId = Long.valueOf(request.get("doctorId").toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid Doctor ID format"));
        }

        LocalDate startDate = LocalDate.parse(request.get("startDate").toString());
        LocalDate endDate = LocalDate.parse(request.get("endDate").toString());
        String leaveType = (String) request.getOrDefault("leaveType", "CASUAL_LEAVE");
        String reason = (String) request.get("reason");

        Map<String, Object> response = leaveService.applyLeave(doctorId, leaveType, startDate, endDate, reason);
        if (response.containsKey("error")) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getDoctorLeaves(@PathVariable Long doctorId) {
        List<LeaveRequest> leaves = leaveService.getDoctorLeaves(doctorId);
        return ResponseEntity.ok(leaves);
    }

    @GetMapping("/pending")
    public ResponseEntity<?> getPendingLeaves() {
        return ResponseEntity.ok(leaveService.getPendingLeaves());
    }

    @PutMapping("/approve/{leaveId}")
    public ResponseEntity<?> reviewLeave(@PathVariable Long leaveId, @RequestBody Map<String, Object> request) {
        String status = (String) request.getOrDefault("status", "APPROVED");
        Map<String, Object> response = leaveService.reviewLeave(leaveId, status);

        if (response.containsKey("error")) {
            return ResponseEntity.badRequest().body(response);
        }

        return ResponseEntity.ok(response);
    }
}
