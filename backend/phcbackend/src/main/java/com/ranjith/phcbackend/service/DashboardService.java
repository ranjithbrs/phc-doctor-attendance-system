package com.ranjith.phcbackend.service;

import java.time.LocalDate;
import java.util.ArrayList;
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

    // ✅ SUMMARY (CARDS)
    public Map<String, Object> getSummary(Long divisionId) {

        Map<String, Object> summary = new HashMap<>();

        List<PHC> phcs = phcRepository.findAll()
                .stream()
                .filter(phc -> phc.getDivision() != null &&
                        phc.getDivision().getId().equals(divisionId))
                .toList();

        int totalPhc = phcs.size();

        List<Doctor> doctors = doctorRepository.findAll()
                .stream()
                .filter(doc ->
                        doc.getPhc() != null &&
                        doc.getPhc().getDivision() != null &&
                        doc.getPhc().getDivision().getId().equals(divisionId) &&
                        !"ADMIN".equals(doc.getRole())
                )
                .toList();

        int totalDoctors = doctors.size();

        LocalDate today = LocalDate.now();

        long presentDoctors = doctors.stream()
                .filter(doc -> {
                    var att = attendanceRepository.findByDoctorAndDate(doc, today);
                    if (att.isEmpty()) return false;
                    String status = att.get().getStatus();
                    return "PRESENT".equals(status) || "COMPLETED".equals(status);
                })
                .count();

        long absentDoctors = totalDoctors - presentDoctors;

        summary.put("totalPhc", totalPhc);
        summary.put("totalDoctors", totalDoctors);
        summary.put("presentDoctors", presentDoctors);
        summary.put("absentDoctors", absentDoctors);

        return summary;
    }

    // ✅ TABLE DATA (PHC OVERVIEW)
    public List<Map<String, Object>> getPhcOverview(Long divisionId) {

        List<Map<String, Object>> result = new ArrayList<>();

        LocalDate today = LocalDate.now();

        List<PHC> phcs = phcRepository.findAll()
                .stream()
                .filter(phc -> phc.getDivision() != null &&
                        phc.getDivision().getId().equals(divisionId))
                .toList();

        for (PHC phc : phcs) {

            List<Doctor> doctors = doctorRepository.findAll()
                    .stream()
                    .filter(doc ->
                            doc.getPhc() != null &&
                            doc.getPhc().getId().equals(phc.getId()) &&
                            !"ADMIN".equals(doc.getRole())
                    )
                    .toList();

            int totalDoctors = doctors.size();

            long present = doctors.stream()
                    .filter(doc -> {
                        var att = attendanceRepository.findByDoctorAndDate(doc, today);
                        if (att == null || att.isEmpty()) return false;
                        String status = att.get().getStatus();
                        return "PRESENT".equals(status) || "COMPLETED".equals(status);
                    })
                    .count();

            long absent = totalDoctors - present;

            double percentage = totalDoctors == 0 ? 0 :
                    (present * 100.0) / totalDoctors;

            Map<String, Object> map = new HashMap<>();
            map.put("phcName", phc.getName());
            map.put("totalDoctors", totalDoctors);
            map.put("present", present);
            map.put("absent", absent);
            map.put("percentage", String.format(java.util.Locale.US, "%.2f", percentage));

            result.add(map);
        }

        return result;
    }
}