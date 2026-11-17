package com.ranjith.phcbackend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.ranjith.phcbackend.model.Doctor;
import com.ranjith.phcbackend.model.LeaveRequest;
import com.ranjith.phcbackend.repository.DoctorRepository;
import com.ranjith.phcbackend.repository.LeaveRequestRepository;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final DoctorRepository doctorRepository;
    private final com.ranjith.phcbackend.repository.AttendanceRepository attendanceRepository;

    public LeaveService(LeaveRequestRepository leaveRepository,
                        DoctorRepository doctorRepository,
                        @org.springframework.beans.factory.annotation.Autowired(required = false) com.ranjith.phcbackend.repository.AttendanceRepository attendanceRepository) {
        this.leaveRepository = leaveRepository;
        this.doctorRepository = doctorRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public LeaveService(LeaveRequestRepository leaveRepository, DoctorRepository doctorRepository) {
        this(leaveRepository, doctorRepository, null);
    }

    public Map<String, Object> applyLeave(Long doctorId, String leaveType, LocalDate startDate, LocalDate endDate, String reason) {
        Map<String, Object> response = new HashMap<>();

        if (doctorId == null || startDate == null || endDate == null) {
            response.put("error", "Doctor ID, start date, and end date are required");
            return response;
        }

        if (endDate.isBefore(startDate)) {
            response.put("error", "End date cannot be earlier than start date");
            return response;
        }

        Optional<Doctor> docOpt = doctorRepository.findById(doctorId);
        if (docOpt.isEmpty()) {
            response.put("error", "Doctor not found");
            return response;
        }

        LeaveRequest leave = new LeaveRequest(
            leaveType != null ? leaveType : "CASUAL_LEAVE",
            startDate,
            endDate,
            reason,
            "PENDING",
            LocalDateTime.now(),
            docOpt.get()
        );

        LeaveRequest saved = leaveRepository.save(leave);

        response.put("message", "Leave application submitted successfully");
        response.put("leaveId", saved.getId());
        response.put("status", saved.getStatus());
        return response;
    }

    public List<LeaveRequest> getDoctorLeaves(Long doctorId) {
        Optional<Doctor> docOpt = doctorRepository.findById(doctorId);
        return docOpt.map(leaveRepository::findByDoctor).orElse(List.of());
    }

    public List<LeaveRequest> getPendingLeaves() {
        return leaveRepository.findByStatus("PENDING");
    }

    public Map<String, Object> reviewLeave(Long leaveId, String status) {
        Map<String, Object> response = new HashMap<>();
        Optional<LeaveRequest> leaveOpt = leaveRepository.findById(leaveId);

        if (leaveOpt.isEmpty()) {
            response.put("error", "Leave request not found");
            return response;
        }

        LeaveRequest leave = leaveOpt.get();
        String updatedStatus = "APPROVED".equalsIgnoreCase(status) ? "APPROVED" : "REJECTED";

        leave.setStatus(updatedStatus);
        leave.setReviewedAt(LocalDateTime.now());
        leaveRepository.save(leave);

        // Reconcile pre-existing ABSENT or blank records for this doctor across the approved date range
        if ("APPROVED".equals(updatedStatus) && leave.getDoctor() != null && attendanceRepository != null) {
            LocalDate cur = leave.getStartDate();
            while (!cur.isAfter(leave.getEndDate())) {
                var attOpt = attendanceRepository.findByDoctorAndDate(leave.getDoctor(), cur);
                if (attOpt.isPresent()) {
                    var att = attOpt.get();
                    if ("ABSENT".equals(att.getStatus())) {
                        att.setStatus("ON_LEAVE");
                        attendanceRepository.save(att);
                    }
                } else {
                    var att = new com.ranjith.phcbackend.model.Attendance();
                    att.setDoctor(leave.getDoctor());
                    att.setDate(cur);
                    att.setStatus("ON_LEAVE");
                    attendanceRepository.save(att);
                }
                cur = cur.plusDays(1);
            }
        }

        response.put("message", "Leave request " + updatedStatus.toLowerCase() + " successfully");
        response.put("leaveId", leave.getId());
        response.put("status", leave.getStatus());
        return response;
    }

    public boolean isDoctorOnApprovedLeave(Doctor doctor, LocalDate date) {
        if (doctor == null || date == null) return false;
        List<LeaveRequest> approvedLeaves = leaveRepository.findByDoctorAndStatus(doctor, "APPROVED");
        if (approvedLeaves == null) return false;

        for (LeaveRequest leave : approvedLeaves) {
            if ((date.isEqual(leave.getStartDate()) || date.isAfter(leave.getStartDate())) &&
                (date.isEqual(leave.getEndDate()) || date.isBefore(leave.getEndDate()))) {
                return true;
            }
        }
        return false;
    }
}
