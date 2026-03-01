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

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    // ✅ CHECK-IN API
    @PostMapping("/checkin")
    public ResponseEntity<?> checkIn(@RequestBody Map<String, Long> request) {

        Long doctorId = request.get("doctorId");

        String result = attendanceService.checkIn(doctorId);

        Map<String, String> response = new HashMap<>();
        response.put("message", result);

        if (result.equals("Check-in successful")) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }
    // ✅ CHECK-OUT API
@PutMapping("/checkout")
public ResponseEntity<?> checkOut(@RequestBody Map<String, Long> request) {

    Long doctorId = request.get("doctorId");

    String result = attendanceService.checkOut(doctorId);

    Map<String, String> response = new HashMap<>();
    response.put("message", result);

    if (result.equals("Check-out successful")) {
        return ResponseEntity.ok(response);
    } else {
        return ResponseEntity.badRequest().body(response);
    }
}
// ✅ TODAY STATUS API
@GetMapping("/status/{doctorId}")
public ResponseEntity<?> getTodayStatus(@PathVariable Long doctorId) {

    String result = attendanceService.getTodayStatus(doctorId);

    Map<String, String> response = new HashMap<>();
    response.put("status", result);

    return ResponseEntity.ok(response);
}
// ✅ ATTENDANCE HISTORY API
@GetMapping("/history/{doctorId}")
public ResponseEntity<?> getHistory(
        @PathVariable Long doctorId,
        @RequestParam String from,
        @RequestParam String to) {

    LocalDate startDate = LocalDate.parse(from);
    LocalDate endDate = LocalDate.parse(to);

    List<Attendance> history =
            attendanceService.getAttendanceHistory(doctorId, startDate, endDate);

    return ResponseEntity.ok(history);
}
}