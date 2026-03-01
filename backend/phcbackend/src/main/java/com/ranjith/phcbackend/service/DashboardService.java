package com.ranjith.phcbackend.service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.ranjith.phcbackend.model.Doctor;
import com.ranjith.phcbackend.model.PHC;
import com.ranjith.phcbackend.repository.AttendanceRepository;
import com.ranjith.phcbackend.repository.DoctorRepository;
import com.ranjith.phcbackend.repository.PHCRepository;

@Service
public class DashboardService {

    private final PHCRepository phcRepository;
    private final DoctorRepository doctorRepository;
    private final AttendanceRepository attendanceRepository;

    public DashboardService(PHCRepository phcRepository,
                            DoctorRepository doctorRepository,
                            AttendanceRepository attendanceRepository) {
        this.phcRepository = phcRepository;
        this.doctorRepository = doctorRepository;
        this.attendanceRepository = attendanceRepository;
    }

    public Map<String, Object> getSummary(Long divisionId) {

        Map<String, Object> summary = new HashMap<>();

        // Total PHCs in division
        List<PHC> phcs = phcRepository.findAll()
                .stream()
                .filter(phc -> phc.getDivision().getId().equals(divisionId))
                .toList();

        int totalPhc = phcs.size();

        // Total Doctors in that division
        List<Doctor> doctors = doctorRepository.findAll()
                .stream()
                .filter(doc -> doc.getPhc().getDivision().getId().equals(divisionId))
                .toList();

        int totalDoctors = doctors.size();

        // Present today
        LocalDate today = LocalDate.now();

        long presentDoctors = doctors.stream()
                .filter(doc ->
                        attendanceRepository
                                .findByDoctorAndDate(doc, today)
                                .isPresent()
                )
                .count();

        long absentDoctors = totalDoctors - presentDoctors;

        summary.put("totalPhc", totalPhc);
        summary.put("totalDoctors", totalDoctors);
        summary.put("presentDoctors", presentDoctors);
        summary.put("absentDoctors", absentDoctors);

        return summary;
    }
}