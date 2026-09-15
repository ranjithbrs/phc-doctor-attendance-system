package com.ranjith.phcbackend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ranjith.phcbackend.model.Attendance;
import com.ranjith.phcbackend.model.Doctor;
import com.ranjith.phcbackend.model.PHC;
import com.ranjith.phcbackend.repository.AttendanceRepository;
import com.ranjith.phcbackend.repository.DoctorRepository;

import java.time.LocalDateTime;
import com.ranjith.phcbackend.model.AttendanceAuditLog;
import com.ranjith.phcbackend.repository.AttendanceAuditLogRepository;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final DoctorRepository doctorRepository;
    private final AttendanceAuditLogRepository auditLogRepository;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            DoctorRepository doctorRepository,
            AttendanceAuditLogRepository auditLogRepository
    ) {
        this.attendanceRepository = attendanceRepository;
        this.doctorRepository = doctorRepository;
        this.auditLogRepository = auditLogRepository;
    }

    // ===== CHECK-IN WITH SECURE GEO-FENCING & GPS VALIDATION & AUDIT LOGGING =====

    public String checkIn(Long doctorId, Double userLat, Double userLng, Double accuracy) {

        if (doctorId == null) {
            return "Invalid doctor ID";
        }

        Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);
        if (doctorOptional.isEmpty()) {
            return "Doctor not found";
        }

        Doctor doctor = doctorOptional.get();
        LocalDate today = LocalDate.now();

        // Validate coordinate existence
        if (userLat == null || userLng == null) {
            logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, null, "REJECTED_INVALID_COORDINATES", "Missing GPS latitude or longitude");
            return "GPS location coordinates (latitude and longitude) are required for check-in.";
        }

        // Validate numeric validity & bounds (-90 to +90 for lat, -180 to +180 for lng)
        if (Double.isNaN(userLat) || Double.isInfinite(userLat) || userLat < -90.0 || userLat > 90.0) {
            logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, null, "REJECTED_INVALID_COORDINATES", "Latitude out of bounds [-90, +90]");
            return "Invalid latitude value. Must be between -90 and +90 degrees.";
        }

        if (Double.isNaN(userLng) || Double.isInfinite(userLng) || userLng < -180.0 || userLng > 180.0) {
            logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, null, "REJECTED_INVALID_COORDINATES", "Longitude out of bounds [-180, +180]");
            return "Invalid longitude value. Must be between -180 and +180 degrees.";
        }

        // Validate GPS Accuracy threshold (Max 200 meters allowed uncertainty)
        final double MAX_ALLOWED_ACCURACY_METERS = 200.0;
        if (accuracy != null) {
            if (Double.isNaN(accuracy) || Double.isInfinite(accuracy) || accuracy < 0) {
                logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, null, "REJECTED_POOR_ACCURACY", "Malformed GPS accuracy value");
                return "Invalid GPS accuracy value.";
            }
            if (accuracy > MAX_ALLOWED_ACCURACY_METERS) {
                logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, null, "REJECTED_POOR_ACCURACY", String.format("GPS uncertainty ±%.0fm exceeds max allowed %.0fm", accuracy, MAX_ALLOWED_ACCURACY_METERS));
                return String.format("GPS accuracy is insufficient (±%.0fm). Maximum allowed uncertainty is %.0fm. Please move to an open area with better reception.", accuracy, MAX_ALLOWED_ACCURACY_METERS);
            }
        }

        // Validate assigned PHC coordinates
        PHC phc = doctor.getPhc();
        if (phc == null || phc.getLatitude() == null || phc.getLongitude() == null) {
            logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, null, "REJECTED_MISSING_PHC_COORDS", "Assigned PHC coordinates missing in DB");
            return "Assigned Primary Health Centre (PHC) coordinates are not configured in the database.";
        }

        Optional<Attendance> existing = attendanceRepository.findByDoctorAndDate(doctor, today);
        if (existing.isPresent()) {
            Attendance attendance = existing.get();
            if ("PRESENT".equals(attendance.getStatus()) || "COMPLETED".equals(attendance.getStatus())) {
                return "Already checked in today";
            }
        }

        Attendance attendance = existing.orElse(new Attendance());
        attendance.setDoctor(doctor);
        attendance.setDate(today);

        // Calculate distance using Haversine formula
        double distanceMeters = calculateDistanceInMeters(userLat, userLng, phc.getLatitude(), phc.getLongitude());
        final double MAX_ALLOWED_DISTANCE_METERS = 500.0; // 500 meters geo-fence radius

        if (distanceMeters > MAX_ALLOWED_DISTANCE_METERS) {
            attendance.setStatus("ABSENT");
            attendanceRepository.save(attendance);
            logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, distanceMeters, "REJECTED_OUTSIDE_RADIUS", String.format("%.0fm away from %s (Max: %.0fm)", distanceMeters, phc.getName(), MAX_ALLOWED_DISTANCE_METERS));
            return String.format("Outside PHC location (%.0fm away from %s). Maximum allowed distance is %.0fm. Attendance marked as ABSENT.", distanceMeters, phc.getName(), MAX_ALLOWED_DISTANCE_METERS);
        }

        attendance.setCheckInTime(LocalTime.now());
        attendance.setStatus("PRESENT");
        attendanceRepository.save(attendance);

        logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, distanceMeters, "VERIFIED_SUCCESS", String.format("Verified inside %s boundary (%.0fm away)", phc.getName(), distanceMeters));
        return String.format("Check-in successful! Verified distance to %s: %.0fm.", phc.getName(), distanceMeters);
    }

    private void logAudit(Doctor doctor, String action, Double lat, Double lng, Double accuracy, Double dist, String result, String remarks) {
        try {
            AttendanceAuditLog log = new AttendanceAuditLog(LocalDateTime.now(), action, lat, lng, accuracy, dist, result, remarks, doctor);
            auditLogRepository.save(log);
        } catch (Exception e) {
            // Log audit failure silently to prevent blocking check-in workflow
            System.err.println("Failed to persist location audit log: " + e.getMessage());
        }
    }

    public List<AttendanceAuditLog> getAuditLogsForDoctor(Long doctorId) {
        Optional<Doctor> doc = doctorRepository.findById(doctorId);
        return doc.map(auditLogRepository::findByDoctorOrderByTimestampDesc).orElse(List.of());
    }

    public List<AttendanceAuditLog> getRecentAuditLogs() {
        return auditLogRepository.findTop50ByOrderByTimestampDesc();
    }

    // ===== CONTINUOUS PRESENCE VERIFICATION (HEARTBEAT PING) =====

    public java.util.Map<String, Object> presencePing(Long doctorId, Double userLat, Double userLng, Double accuracy) {
        java.util.Map<String, Object> response = new java.util.HashMap<>();

        if (doctorId == null) {
            response.put("status", "REJECTED");
            response.put("message", "Invalid doctor ID");
            return response;
        }

        Optional<Doctor> docOpt = doctorRepository.findById(doctorId);
        if (docOpt.isEmpty()) {
            response.put("status", "REJECTED");
            response.put("message", "Doctor not found");
            return response;
        }

        Doctor doctor = docOpt.get();
        LocalDate today = LocalDate.now();

        Optional<Attendance> attendanceOpt = attendanceRepository.findByDoctorAndDate(doctor, today);
        if (attendanceOpt.isEmpty() || !"PRESENT".equals(attendanceOpt.get().getStatus())) {
            response.put("status", "NO_SESSION");
            response.put("message", "No active check-in session found for today.");
            return response;
        }

        Attendance attendance = attendanceOpt.get();

        if (userLat == null || userLng == null || Double.isNaN(userLat) || Double.isInfinite(userLat) ||
            userLat < -90.0 || userLat > 90.0 || Double.isNaN(userLng) || Double.isInfinite(userLng) ||
            userLng < -180.0 || userLng > 180.0) {
            logAudit(doctor, "PRESENCE_PING", userLat, userLng, accuracy, null, "REJECTED_INVALID_COORDINATES", "Invalid GPS coordinates during presence ping");
            response.put("status", "REJECTED");
            response.put("message", "Invalid GPS coordinates during presence ping.");
            return response;
        }

        if (accuracy != null && accuracy > 200.0) {
            logAudit(doctor, "PRESENCE_PING", userLat, userLng, accuracy, null, "REJECTED_POOR_ACCURACY", String.format("Presence ping skipped: uncertainty ±%.0fm > 200m", accuracy));
            response.put("status", "POOR_ACCURACY");
            response.put("message", String.format("Presence ping skipped due to low accuracy (±%.0fm).", accuracy));
            return response;
        }

        PHC phc = doctor.getPhc();
        if (phc == null || phc.getLatitude() == null || phc.getLongitude() == null) {
            response.put("status", "REJECTED");
            response.put("message", "Assigned PHC coordinates not configured.");
            return response;
        }

        double distanceMeters = calculateDistanceInMeters(userLat, userLng, phc.getLatitude(), phc.getLongitude());
        final double MAX_ALLOWED = 500.0;

        LocalTime now = LocalTime.now();
        attendance.setLastPresencePingTime(now);

        if (distanceMeters > MAX_ALLOWED) {
            int breaches = attendance.getPresenceBreachCount() + 1;
            attendance.setPresenceBreachCount(breaches);
            attendanceRepository.save(attendance);

            logAudit(doctor, "PRESENCE_PING", userLat, userLng, accuracy, distanceMeters, "PRESENCE_BREACH_WARNING", String.format("Presence breach detected: %.0fm away from %s (Breach #%d)", distanceMeters, phc.getName(), breaches));

            response.put("status", "BREACH_WARNING");
            response.put("distanceMeters", distanceMeters);
            response.put("breachCount", breaches);
            response.put("message", String.format("Presence warning: You are %.0fm away from %s boundary.", distanceMeters, phc.getName()));
            return response;
        }

        attendanceRepository.save(attendance);

        logAudit(doctor, "PRESENCE_PING", userLat, userLng, accuracy, distanceMeters, "PRESENCE_VERIFIED", String.format("Continuous presence verified inside %s (%.0fm away)", phc.getName(), distanceMeters));

        response.put("status", "VERIFIED");
        response.put("distanceMeters", distanceMeters);
        response.put("lastPingTime", now.toString());
        response.put("message", String.format("Continuous presence verified at %s (%.0fm).", phc.getName(), distanceMeters));
        return response;
    }

    public String checkIn(Long doctorId, Double userLat, Double userLng) {
        return checkIn(doctorId, userLat, userLng, null);
    }

    public String checkIn(Long doctorId) {
        return checkIn(doctorId, null, null, null);
    }

    /**
     * Haversine formula to calculate distance in meters between two GPS coordinates
     */
    private double calculateDistanceInMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // Earth radius in meters
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    // ===== CHECK-OUT =====

    public String checkOut(Long doctorId) {

        if (doctorId == null) {

            return "Invalid doctor ID";

        }

        Optional<Doctor> doctorOptional =
                doctorRepository.findById(doctorId);

        if (doctorOptional.isEmpty()) {

            return "Doctor not found";

        }

        Doctor doctor =
                doctorOptional.get();

        LocalDate today =
                LocalDate.now();

        Optional<Attendance> attendanceOptional =

                attendanceRepository
                        .findByDoctorAndDate(
                                doctor,
                                today
                        );

        if (attendanceOptional.isEmpty()) {
            return "No check-in found for today";
        }

        Attendance attendance = attendanceOptional.get();

        if ("ABSENT".equals(attendance.getStatus())) {
            return "Cannot check out: You were marked ABSENT for today due to being outside the PHC geo-fence.";
        }

        if ("COMPLETED".equals(attendance.getStatus())) {
            return "Already checked out today";
        }

        if (!"PRESENT".equals(attendance.getStatus())) {
            return "No active check-in found for today";
        }

        attendance.setCheckOutTime(LocalTime.now());
        attendance.setStatus("COMPLETED");

        attendanceRepository.save(attendance);

        return "Check-out successful";
    }

    // ===== MARK ABSENT =====

    public void markAbsent(Long doctorId) {

        if (doctorId == null) {

            return;

        }

        Optional<Doctor> doctorOptional =
                doctorRepository.findById(doctorId);

        if (doctorOptional.isEmpty()) {

            return;

        }

        Doctor doctor =
                doctorOptional.get();

        LocalDate today =
                LocalDate.now();

        Optional<Attendance> existing =

                attendanceRepository
                        .findByDoctorAndDate(
                                doctor,
                                today
                        );

        // Avoid duplicate ABSENT entry

        if (existing.isPresent()) {

            return;

        }

        Attendance attendance =
                new Attendance();

        attendance.setDoctor(doctor);

        attendance.setDate(today);

        attendance.setStatus("ABSENT");

        attendanceRepository.save(attendance);

    }

    // ===== TODAY STATUS =====

    public String getTodayStatus(Long doctorId) {

        if (doctorId == null) {

            return "NOT_CHECKED";

        }

        Optional<Doctor> doctorOptional =
                doctorRepository.findById(doctorId);

        if (doctorOptional.isEmpty()) {

            return "NOT_CHECKED";

        }

        Doctor doctor =
                doctorOptional.get();

        LocalDate today =
                LocalDate.now();

        Optional<Attendance> attendance =

                attendanceRepository
                        .findByDoctorAndDate(
                                doctor,
                                today
                        );

        if (attendance.isPresent()) {

            return attendance
                    .get()
                    .getStatus();

        }

        return "NOT_CHECKED";

    }

    // ===== FULL HISTORY =====

    public List<Attendance> getFullHistory(
            Long doctorId
    ) {

        if (doctorId == null) {

            return List.of();

        }

        Optional<Doctor> doctorOptional =
                doctorRepository.findById(doctorId);

        if (doctorOptional.isEmpty()) {

            return List.of();

        }

        return attendanceRepository.findByDoctor(
                doctorOptional.get()
        );

    }

    // ===== FILTERED HISTORY =====

    public List<Attendance> getAttendanceHistory(

            Long doctorId,

            LocalDate startDate,

            LocalDate endDate

    ) {

        if (
            doctorId == null
            ||
            startDate == null
            ||
            endDate == null
        ) {

            return List.of();

        }

        Optional<Doctor> doctorOptional =
                doctorRepository.findById(doctorId);

        if (doctorOptional.isEmpty()) {

            return List.of();

        }

        return attendanceRepository
                .findByDoctorAndDateBetween(

                        doctorOptional.get(),

                        startDate,

                        endDate

                );

    }

}