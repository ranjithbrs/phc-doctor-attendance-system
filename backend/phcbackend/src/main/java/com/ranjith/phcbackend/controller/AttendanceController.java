package com.ranjith.phcbackend.controller;

import java.time.LocalDate;
import java.util.HashMap;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ranjith.phcbackend.model.Attendance;
import com.ranjith.phcbackend.service.AttendanceService;

@RestController
@RequestMapping("/attendance")
@CrossOrigin(origins = "*")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(
            AttendanceService attendanceService
    ) {

        this.attendanceService = attendanceService;

    }

    // ===== CHECK-IN =====

    @PostMapping("/checkin")

    public ResponseEntity<?> checkIn(
            @RequestBody Map<String, Object> request
    ) {

        if (request == null || request.get("doctorId") == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Doctor ID is required"));
        }

        Long doctorId;
        try {
            doctorId = Long.valueOf(request.get("doctorId").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid Doctor ID format"));
        }

        Double latitude = request.get("latitude") != null ? Double.valueOf(request.get("latitude").toString()) : null;
        Double longitude = request.get("longitude") != null ? Double.valueOf(request.get("longitude").toString()) : null;

        String result = attendanceService.checkIn(doctorId, latitude, longitude);

        return ResponseEntity.ok(Map.of("message", result));
    }

    // ===== CHECK-OUT =====

    @PutMapping("/checkout")
    public ResponseEntity<?> checkOut(
            @RequestBody Map<String, Object> request
    ) {

        if (request == null || request.get("doctorId") == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Doctor ID is required"));
        }

        Long doctorId;
        try {
            doctorId = Long.valueOf(request.get("doctorId").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid Doctor ID format"));
        }

        String result = attendanceService.checkOut(doctorId);

        return ResponseEntity.ok(Map.of("message", result));
    }

    // ===== MARK ABSENT =====

    @PostMapping("/absent")
    public ResponseEntity<?> markAbsent(
            @RequestBody Map<String, Object> request
    ) {

        if (request == null || request.get("doctorId") == null) {
            return ResponseEntity.badRequest().body(Map.of("message", "Doctor ID is required"));
        }

        Long doctorId;
        try {
            doctorId = Long.valueOf(request.get("doctorId").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("message", "Invalid Doctor ID format"));
        }

        attendanceService.markAbsent(doctorId);

        return ResponseEntity.ok(Map.of("message", "Doctor marked absent successfully"));
    }

    // ===== TODAY STATUS =====

    @GetMapping("/status/{doctorId}")
    public ResponseEntity<?> getTodayStatus(
            @PathVariable Long doctorId
    ) {

        String status = attendanceService.getTodayStatus(doctorId);
        Map<String, String> response = new HashMap<>();
        response.put("status", status);

        return ResponseEntity.ok(response);
    }

    // ===== HISTORY =====

    @GetMapping("/history/{doctorId}")
    public ResponseEntity<?> getHistory(
            @PathVariable Long doctorId,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {

        if (from != null && to != null) {
            try {
                LocalDate startDate = LocalDate.parse(from);
                LocalDate endDate = LocalDate.parse(to);

                List<Attendance> filteredHistory = attendanceService.getAttendanceHistory(
                        doctorId, startDate, endDate
                );

                return ResponseEntity.ok(filteredHistory);
            } catch (Exception e) {
                return ResponseEntity.badRequest().body(Map.of("message", "Invalid date format. Expected YYYY-MM-DD"));
            }
        }

        List<Attendance> fullHistory = attendanceService.getFullHistory(doctorId);

        return ResponseEntity.ok(fullHistory);
    }

}