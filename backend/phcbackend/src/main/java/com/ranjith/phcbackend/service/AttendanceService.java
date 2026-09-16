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
    private LeaveService leaveService;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            DoctorRepository doctorRepository,
            AttendanceAuditLogRepository auditLogRepository
    ) {
        this.attendanceRepository = attendanceRepository;
        this.doctorRepository = doctorRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    public void setLeaveService(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    // ===== CHECK-IN WITH SECURE GEO-FENCING & GPS VALIDATION & AUDIT LOGGING =====

    public String checkIn(Long doctorId, Double userLat, Double userLng, Double accuracy) {
        return checkIn(doctorId, userLat, userLng, accuracy, null, null);
    }

    public String checkIn(Long doctorId, Double userLat, Double userLng, Double accuracy, Double livenessScore, String photoProof) {
        return checkIn(doctorId, userLat, userLng, accuracy, LocalTime.now(), LocalTime.of(9, 0), 15, livenessScore, photoProof);
    }

    // Overload for custom shift timing / test evaluation
    public String checkIn(Long doctorId, Double userLat, Double userLng, Double accuracy, LocalTime customCheckInTime, LocalTime shiftStartTime, Integer graceMinutes) {
        return checkIn(doctorId, userLat, userLng, accuracy, customCheckInTime, shiftStartTime, graceMinutes, null, null);
    }

    public String checkIn(Long doctorId, Double userLat, Double userLng, Double accuracy, LocalTime customCheckInTime, LocalTime shiftStartTime, Integer graceMinutes, Double livenessScore, String photoProof) {
        if (doctorId == null) return "Invalid doctor ID";
        Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);
        if (doctorOptional.isEmpty()) return "Doctor not found";

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

        // Validate Facial Liveness Score if provided
        if (livenessScore != null && livenessScore < 0.70) {
            logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, null, "REJECTED_LOW_LIVENESS",
                     String.format("Facial liveness verification failed (Score: %.2f < 0.70)", livenessScore));
            return String.format("Facial liveness verification failed (Liveness Score: %.2f < 0.70). Please capture a clear real-time selfie.", livenessScore);
        }

        // Anti-spoofing: Evaluate location integrity (impossible speed / teleportation detection)
        String integrityWarning = evaluateLocationIntegrity(doctor, userLat, userLng, accuracy);
        if (integrityWarning != null) {
            return integrityWarning;
        }

        PHC phc = doctor.getPhc();
        if (phc == null || phc.getLatitude() == null || phc.getLongitude() == null) {
            logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, null, "REJECTED_MISSING_PHC_COORDS", "Assigned PHC coordinates missing in DB");
            return "Assigned Primary Health Centre (PHC) coordinates are not configured in the database.";
        }

        Optional<Attendance> existing = attendanceRepository.findByDoctorAndDate(doctor, today);
        if (existing.isPresent()) {
            Attendance attendance = existing.get();
            if ("PRESENT".equals(attendance.getStatus()) || "LATE".equals(attendance.getStatus()) || "COMPLETED".equals(attendance.getStatus()) || "COMPLETED_EARLY".equals(attendance.getStatus())) {
                return "Already checked in today";
            }
        }

        Attendance attendance = existing.orElse(new Attendance());
        attendance.setDoctor(doctor);
        attendance.setDate(today);

        if (livenessScore != null) {
            attendance.setLivenessScore(livenessScore);
        }
        if (photoProof != null) {
            attendance.setPhotoProof(photoProof);
        }

        double distanceMeters = calculateDistanceInMeters(userLat, userLng, phc.getLatitude(), phc.getLongitude());
        double maxAllowedDistance = phc.getRadiusMeters() != null ? phc.getRadiusMeters() : 500.0;

        if (distanceMeters > maxAllowedDistance) {
            attendance.setStatus("ABSENT");
            attendanceRepository.save(attendance);
            logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, distanceMeters, "REJECTED_OUTSIDE_RADIUS", String.format("%.0fm away from %s (Max: %.0fm)", distanceMeters, phc.getName(), maxAllowedDistance));
            return String.format("Outside PHC location (%.0fm away from %s). Maximum allowed distance is %.0fm. Attendance marked as ABSENT.", distanceMeters, phc.getName(), maxAllowedDistance);
        }

        LocalTime checkTime = customCheckInTime != null ? customCheckInTime : LocalTime.now();
        LocalTime shiftTime = shiftStartTime != null ? shiftStartTime : LocalTime.of(9, 0);
        int grace = graceMinutes != null ? graceMinutes : 15;
        LocalTime graceCutoff = shiftTime.plusMinutes(grace);

        String computedStatus = checkTime.isAfter(graceCutoff) ? "LATE" : "PRESENT";

        attendance.setCheckInTime(checkTime);
        attendance.setStatus(computedStatus);
        attendanceRepository.save(attendance);

        String auditRemark = "LATE".equals(computedStatus)
                ? String.format("Verified inside %s boundary (%.0fm away). Checked in late at %s (Grace Cutoff: %s). Liveness: %.2f", phc.getName(), distanceMeters, checkTime, graceCutoff, livenessScore != null ? livenessScore : 0.95)
                : String.format("Verified inside %s boundary (%.0fm away). Status: %s. Liveness: %.2f", phc.getName(), distanceMeters, computedStatus, livenessScore != null ? livenessScore : 0.95);

        logAudit(doctor, "CHECK_IN_ATTEMPT", userLat, userLng, accuracy, distanceMeters, "VERIFIED_SUCCESS", auditRemark);

        if ("LATE".equals(computedStatus)) {
            return String.format("Check-in successful! (Marked LATE - checked in at %s, grace cutoff was %s). Verified distance to %s: %.0fm.", checkTime, graceCutoff, phc.getName(), distanceMeters);
        }
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

    // ===== ANTI-SPOOFING & LOCATION INTEGRITY HEURISTICS =====

    private String evaluateLocationIntegrity(Doctor doctor, Double currentLat, Double currentLng, Double accuracy) {
        try {
            List<AttendanceAuditLog> recentLogs = auditLogRepository.findByDoctorOrderByTimestampDesc(doctor);
            if (recentLogs == null || recentLogs.isEmpty()) {
                return null;
            }

            AttendanceAuditLog prevLog = null;
            for (AttendanceAuditLog l : recentLogs) {
                if (l.getLatitude() != null && l.getLongitude() != null) {
                    prevLog = l;
                    break;
                }
            }

            if (prevLog == null) {
                return null;
            }

            long timeDiffSeconds = java.time.Duration.between(prevLog.getTimestamp(), LocalDateTime.now()).getSeconds();
            if (timeDiffSeconds <= 0) {
                timeDiffSeconds = 1;
            }

            if (timeDiffSeconds <= 14400) { // Check within 4-hour window
                double distanceMeters = calculateDistanceInMeters(prevLog.getLatitude(), prevLog.getLongitude(), currentLat, currentLng);
                double speedMetersPerSec = distanceMeters / timeDiffSeconds;
                double speedKmPerHour = speedMetersPerSec * 3.6;

                final double MAX_HUMAN_SPEED_KMH = 250.0; // Max allowed ground speed (250 km/h)

                if (distanceMeters > 500 && speedKmPerHour > MAX_HUMAN_SPEED_KMH) {
                    logAudit(doctor, "LOCATION_INTEGRITY_CHECK", currentLat, currentLng, accuracy, distanceMeters,
                             "FLAGGED_IMPOSSIBLE_SPEED",
                             String.format("Impossible travel speed detected: %.0f km/h (%.0fm in %ds)", speedKmPerHour, distanceMeters, timeDiffSeconds));
                    return String.format("Location integrity alert: Impossible travel speed detected (%.0f km/h). Check-in rejected due to suspected GPS manipulation.", speedKmPerHour);
                }
            }
        } catch (Exception e) {
            System.err.println("Location integrity evaluation error: " + e.getMessage());
        }
        return null;
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
        double maxAllowedDistance = phc.getRadiusMeters() != null ? phc.getRadiusMeters() : 500.0;

        LocalTime now = LocalTime.now();
        attendance.setLastPresencePingTime(now);

        if (distanceMeters > maxAllowedDistance) {
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

        if ("COMPLETED".equals(attendance.getStatus()) || "COMPLETED_EARLY".equals(attendance.getStatus())) {
            return "Already checked out today";
        }

        if (!"PRESENT".equals(attendance.getStatus()) && !"LATE".equals(attendance.getStatus())) {
            return "No active check-in found for today";
        }

        LocalTime now = LocalTime.now();
        attendance.setCheckOutTime(now);

        long workedMinutes = 0;
        if (attendance.getCheckInTime() != null) {
            workedMinutes = java.time.Duration.between(attendance.getCheckInTime(), now).toMinutes();
        }

        if (workedMinutes > 0 && workedMinutes < 240) { // Under 4 hours
            attendance.setStatus("COMPLETED_EARLY");
            attendanceRepository.save(attendance);
            logAudit(doctor, "CHECK_OUT_ATTEMPT", null, null, null, null, "CHECK_OUT_SUCCESS", String.format("Checked out early after %d mins worked", workedMinutes));
            return String.format("Check-out successful (Early Departure: %d mins worked)", workedMinutes);
        }

        attendance.setStatus("COMPLETED");
        attendanceRepository.save(attendance);
        logAudit(doctor, "CHECK_OUT_ATTEMPT", null, null, null, null, "CHECK_OUT_SUCCESS", String.format("Checked out after %d mins worked", workedMinutes));
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

    // ===== AUTOMATED ABSENTEE ALERTS =====

    public int runAutomatedAbsenteeCheck() {
        List<Doctor> doctors = doctorRepository.findAll();
        LocalDate today = LocalDate.now();
        int newlyFlaggedAbsent = 0;

        for (Doctor doctor : doctors) {
            Optional<Attendance> existing = attendanceRepository.findByDoctorAndDate(doctor, today);
            if (existing.isEmpty()) {
                boolean onApprovedLeave = leaveService != null && leaveService.isDoctorOnApprovedLeave(doctor, today);

                Attendance attendance = new Attendance();
                attendance.setDoctor(doctor);
                attendance.setDate(today);
                attendance.setStatus(onApprovedLeave ? "ON_LEAVE" : "ABSENT");
                attendanceRepository.save(attendance);

                if (onApprovedLeave) {
                    logAudit(doctor, "AUTOMATED_ABSENTEE_CHECK", null, null, null, null,
                             "AUTOMATED_LEAVE_EXEMPTION", "Doctor exempted from absentee alert due to approved leave");
                } else {
                    logAudit(doctor, "AUTOMATED_ABSENTEE_CHECK", null, null, null, null,
                             "AUTOMATED_ABSENTEE_ALERT", "Doctor failed to check-in by cut-off time");
                    newlyFlaggedAbsent++;
                }
            }
        }
        return newlyFlaggedAbsent;
    }

    public List<java.util.Map<String, Object>> getAbsenteeAlerts() {
        List<Doctor> doctors = doctorRepository.findAll();
        LocalDate today = LocalDate.now();
        List<java.util.Map<String, Object>> alerts = new java.util.ArrayList<>();

        for (Doctor doctor : doctors) {
            boolean onApprovedLeave = leaveService != null && leaveService.isDoctorOnApprovedLeave(doctor, today);
            if (onApprovedLeave) {
                continue; // Skip doctors on approved leave from absentee alert list
            }

            Optional<Attendance> existing = attendanceRepository.findByDoctorAndDate(doctor, today);
            boolean isAbsent = existing.isEmpty() || "ABSENT".equals(existing.get().getStatus());

            if (isAbsent) {
                java.util.Map<String, Object> alert = new java.util.HashMap<>();
                alert.put("doctorId", doctor.getId());
                alert.put("doctorName", doctor.getName());
                alert.put("doctorEmail", doctor.getEmail());
                alert.put("phcName", doctor.getPhc() != null ? doctor.getPhc().getName() : "Unassigned");
                alert.put("date", today.toString());
                alert.put("status", existing.isPresent() ? existing.get().getStatus() : "UNREPORTED");
                alert.put("alertType", "AUTOMATED_ABSENTEE_ALERT");

                // Attach last audit log for context if available
                List<AttendanceAuditLog> auditLogs = auditLogRepository.findByDoctorOrderByTimestampDesc(doctor);
                if (auditLogs != null && !auditLogs.isEmpty()) {
                    AttendanceAuditLog lastLog = auditLogs.get(0);
                    alert.put("lastAuditResult", lastLog.getVerificationResult());
                    alert.put("lastAuditRemarks", lastLog.getRemarks());
                    alert.put("lastAttemptTime", lastLog.getTimestamp().toString());
                } else {
                    alert.put("lastAuditResult", "NO_ATTEMPT");
                    alert.put("lastAuditRemarks", "No check-in attempt recorded today");
                    alert.put("lastAttemptTime", null);
                }

                alerts.add(alert);
            }
        }

        return alerts;
    }

    // ===== OFFLINE SYNCHRONIZATION =====

    public List<java.util.Map<String, Object>> syncOfflineRecords(Long doctorId, List<java.util.Map<String, Object>> records) {
        List<java.util.Map<String, Object>> results = new java.util.ArrayList<>();
        if (doctorId == null || records == null || records.isEmpty()) {
            return results;
        }

        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            return results;
        }

        Doctor doctor = doctorOpt.get();

        for (java.util.Map<String, Object> rec : records) {
            java.util.Map<String, Object> res = new java.util.HashMap<>();
            Object offlineId = rec.get("offlineId");
            res.put("offlineId", offlineId != null ? offlineId.toString() : "N/A");

            String eventType = rec.get("eventType") != null ? rec.get("eventType").toString() : "CHECK_IN";
            Double lat = rec.get("latitude") != null ? Double.valueOf(rec.get("latitude").toString()) : null;
            Double lng = rec.get("longitude") != null ? Double.valueOf(rec.get("longitude").toString()) : null;
            Double accuracy = rec.get("accuracy") != null ? Double.valueOf(rec.get("accuracy").toString()) : null;

            LocalTime eventTime = LocalTime.now();
            if (rec.get("timestamp") != null) {
                try {
                    String ts = rec.get("timestamp").toString();
                    if (ts.contains("T")) {
                        eventTime = java.time.LocalDateTime.parse(ts).toLocalTime();
                    } else {
                        eventTime = LocalTime.parse(ts);
                    }
                } catch (Exception e) {
                    // Fallback to current time if parsing fails
                }
            }

            if ("CHECK_IN".equalsIgnoreCase(eventType)) {
                String checkInRes = checkIn(doctorId, lat, lng, accuracy, eventTime, LocalTime.of(9, 0), 15);
                if (checkInRes != null && checkInRes.startsWith("Check-in successful")) {
                    logAudit(doctor, "OFFLINE_SYNC_CHECK_IN", lat, lng, accuracy, null, "OFFLINE_SYNC_SUCCESS", "Offline check-in synced successfully");
                    res.put("status", "SUCCESS");
                    res.put("message", checkInRes);
                } else {
                    logAudit(doctor, "OFFLINE_SYNC_CHECK_IN", lat, lng, accuracy, null, "OFFLINE_SYNC_REJECTED", checkInRes);
                    res.put("status", "REJECTED");
                    res.put("message", checkInRes);
                }
            } else if ("PRESENCE_PING".equalsIgnoreCase(eventType)) {
                java.util.Map<String, Object> pingRes = presencePing(doctorId, lat, lng, accuracy);
                logAudit(doctor, "OFFLINE_SYNC_PING", lat, lng, accuracy, null, "OFFLINE_SYNC_PING", "Offline presence ping synced");
                res.put("status", pingRes.get("status"));
                res.put("message", pingRes.get("message"));
            } else {
                res.put("status", "UNKNOWN_EVENT");
                res.put("message", "Unsupported event type: " + eventType);
            }

            results.add(res);
        }

        return results;
    }

    // ===== ANOMALY DETECTION ENGINE =====

    public List<java.util.Map<String, Object>> getAttendanceAnomalies() {
        List<java.util.Map<String, Object>> anomalies = new java.util.ArrayList<>();
        List<Doctor> doctors = doctorRepository.findAll();

        for (Doctor doctor : doctors) {
            // 1. Check for location integrity / spoofing alerts in audit logs
            List<AttendanceAuditLog> auditLogs = auditLogRepository.findByDoctorOrderByTimestampDesc(doctor);
            if (auditLogs != null) {
                for (AttendanceAuditLog log : auditLogs) {
                    if ("FLAGGED_IMPOSSIBLE_SPEED".equals(log.getVerificationResult())) {
                        java.util.Map<String, Object> anomaly = new java.util.HashMap<>();
                        anomaly.put("doctorId", doctor.getId());
                        anomaly.put("doctorName", doctor.getName());
                        anomaly.put("phcName", doctor.getPhc() != null ? doctor.getPhc().getName() : "Unassigned");
                        anomaly.put("anomalyType", "SUSPECTED_GPS_SPOOFING");
                        anomaly.put("severity", "CRITICAL");
                        anomaly.put("description", "Impossible travel velocity detected between GPS fixes");
                        anomaly.put("evidence", log.getRemarks());
                        anomaly.put("detectedAt", log.getTimestamp().toString());
                        anomalies.add(anomaly);
                        break;
                    }
                }
            }

            // 2. Check for high presence breach count & frequent late check-ins in attendance history
            List<Attendance> attendances = attendanceRepository.findByDoctor(doctor);
            if (attendances != null) {
                int lateCount = 0;
                for (Attendance att : attendances) {
                    if (att.getPresenceBreachCount() != null && att.getPresenceBreachCount() >= 3) {
                        java.util.Map<String, Object> anomaly = new java.util.HashMap<>();
                        anomaly.put("doctorId", doctor.getId());
                        anomaly.put("doctorName", doctor.getName());
                        anomaly.put("phcName", doctor.getPhc() != null ? doctor.getPhc().getName() : "Unassigned");
                        anomaly.put("anomalyType", "HIGH_PRESENCE_BREACHES");
                        anomaly.put("severity", "HIGH");
                        anomaly.put("description", String.format("%d continuous presence geo-fence breaches recorded on %s", att.getPresenceBreachCount(), att.getDate()));
                        anomaly.put("evidence", String.format("Breaches: %d", att.getPresenceBreachCount()));
                        anomaly.put("detectedAt", att.getDate().toString());
                        anomalies.add(anomaly);
                    }

                    if ("LATE".equals(att.getStatus())) {
                        lateCount++;
                    }
                }

                if (lateCount >= 2) {
                    java.util.Map<String, Object> anomaly = new java.util.HashMap<>();
                    anomaly.put("doctorId", doctor.getId());
                    anomaly.put("doctorName", doctor.getName());
                    anomaly.put("phcName", doctor.getPhc() != null ? doctor.getPhc().getName() : "Unassigned");
                    anomaly.put("anomalyType", "FREQUENT_LATE_ARRIVALS");
                    anomaly.put("severity", "MEDIUM");
                    anomaly.put("description", String.format("%d late check-ins recorded in attendance history", lateCount));
                    anomaly.put("evidence", String.format("Total late days: %d", lateCount));
                    anomaly.put("detectedAt", LocalDate.now().toString());
                    anomalies.add(anomaly);
                }
            }
        }

        return anomalies;
    }

}