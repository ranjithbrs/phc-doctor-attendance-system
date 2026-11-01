package com.ranjith.phcbackend;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ranjith.phcbackend.model.Attendance;
import com.ranjith.phcbackend.model.AttendanceAuditLog;
import com.ranjith.phcbackend.model.Doctor;
import com.ranjith.phcbackend.model.LeaveRequest;
import com.ranjith.phcbackend.model.PHC;
import com.ranjith.phcbackend.repository.AttendanceAuditLogRepository;
import com.ranjith.phcbackend.repository.AttendanceRepository;
import com.ranjith.phcbackend.repository.DoctorRepository;
import com.ranjith.phcbackend.repository.LeaveRequestRepository;
import com.ranjith.phcbackend.repository.PHCRepository;
import com.ranjith.phcbackend.service.AttendanceService;
import com.ranjith.phcbackend.service.LeaveService;

class AttendanceValidationTest {

    private AttendanceRepository attendanceRepository;
    private DoctorRepository doctorRepository;
    private AttendanceAuditLogRepository auditLogRepository;
    private AttendanceService attendanceService;
    private Doctor sampleDoctor;

    @BeforeEach
    void setUp() {
        attendanceRepository = Mockito.mock(AttendanceRepository.class);
        doctorRepository = Mockito.mock(DoctorRepository.class);
        auditLogRepository = Mockito.mock(AttendanceAuditLogRepository.class);
        attendanceService = new AttendanceService(attendanceRepository, doctorRepository, auditLogRepository);

        PHC phc = new PHC("Thudiyalur PHC", "Coimbatore", "PHC", 11.0168, 76.9558, null);
        sampleDoctor = new Doctor("Dr. Arunkumar", "doctor@phc.gov.in", "doc123", "Physician", "DOCTOR", phc);

        Mockito.when(doctorRepository.findById(1L)).thenReturn(Optional.of(sampleDoctor));
    }

    @Test
    void testValidCheckInWithinGeoFence() {
        String result = attendanceService.checkIn(1L, 11.0168, 76.9558, 10.0);
        assertTrue(result.startsWith("Check-in successful!"));
        verify(auditLogRepository).save(any(AttendanceAuditLog.class));
    }

    @Test
    void testCheckInOutsideGeoFence() {
        String result = attendanceService.checkIn(1L, 13.0827, 80.2707, 10.0);
        assertTrue(result.contains("Outside PHC location") && result.contains("ABSENT"));
        verify(auditLogRepository).save(any(AttendanceAuditLog.class));
    }

    @Test
    void testCheckInMissingCoordinates() {
        String result = attendanceService.checkIn(1L, null, null, 10.0);
        assertEquals("GPS location coordinates (latitude and longitude) are required for check-in.", result);
        verify(auditLogRepository).save(any(AttendanceAuditLog.class));
    }

    @Test
    void testCheckInInvalidLatitude() {
        String result = attendanceService.checkIn(1L, 100.0, 76.9558, 10.0);
        assertEquals("Invalid latitude value. Must be between -90 and +90 degrees.", result);
        verify(auditLogRepository).save(any(AttendanceAuditLog.class));
    }

    @Test
    void testCheckInInvalidLongitude() {
        String result = attendanceService.checkIn(1L, 11.0168, -200.0, 10.0);
        assertEquals("Invalid longitude value. Must be between -180 and +180 degrees.", result);
        verify(auditLogRepository).save(any(AttendanceAuditLog.class));
    }

    @Test
    void testCheckInPoorAccuracyThreshold() {
        String result = attendanceService.checkIn(1L, 11.0168, 76.9558, 350.0);
        assertTrue(result.contains("GPS accuracy is insufficient"));
        verify(auditLogRepository).save(any(AttendanceAuditLog.class));
    }

    @Test
    void testPresencePingVerified() {
        Attendance presentAttendance = new Attendance(LocalDate.now(), LocalTime.now(), null, "PRESENT", sampleDoctor);
        Mockito.when(attendanceRepository.findByDoctorAndDate(sampleDoctor, LocalDate.now())).thenReturn(Optional.of(presentAttendance));

        Map<String, Object> response = attendanceService.presencePing(1L, 11.0168, 76.9558, 15.0);
        assertEquals("VERIFIED", response.get("status"));
    }

    @Test
    void testPresencePingBreachWarning() {
        Attendance presentAttendance = new Attendance(LocalDate.now(), LocalTime.now(), null, "PRESENT", sampleDoctor);
        Mockito.when(attendanceRepository.findByDoctorAndDate(sampleDoctor, LocalDate.now())).thenReturn(Optional.of(presentAttendance));

        Map<String, Object> response = attendanceService.presencePing(1L, 13.0827, 80.2707, 15.0);
        assertEquals("BREACH_WARNING", response.get("status"));
    }

    @Test
    void testAntiSpoofingImpossibleSpeed() {
        // Previous log in Chennai 2 minutes ago
        AttendanceAuditLog prevLog = new AttendanceAuditLog(LocalDateTime.now().minusMinutes(2), "CHECK_IN_ATTEMPT", 13.0827, 80.2707, 10.0, 500000.0, "VERIFIED_SUCCESS", "Initial fix", sampleDoctor);
        Mockito.when(auditLogRepository.findByDoctorOrderByTimestampDesc(sampleDoctor)).thenReturn(List.of(prevLog));

        // Current attempt in Coimbatore (500 km away within 2 mins -> ~15,000 km/h)
        String result = attendanceService.checkIn(1L, 11.0168, 76.9558, 10.0);
        assertTrue(result.contains("Impossible travel speed detected") || result.contains("Location integrity alert"));
    }

    @Test
    void testAutomatedAbsenteeAlerts() {
        Mockito.when(doctorRepository.findAll()).thenReturn(List.of(sampleDoctor));
        Mockito.when(attendanceRepository.findByDoctorAndDate(sampleDoctor, LocalDate.now())).thenReturn(Optional.empty());

        int newlyFlagged = attendanceService.runAutomatedAbsenteeCheck();
        assertEquals(1, newlyFlagged);
        verify(attendanceRepository).save(any(Attendance.class));
        verify(auditLogRepository).save(any(AttendanceAuditLog.class));

        List<Map<String, Object>> alerts = attendanceService.getAbsenteeAlerts();
        assertFalse(alerts.isEmpty());
        assertEquals("AUTOMATED_ABSENTEE_ALERT", alerts.get(0).get("alertType"));
    }

    @Test
    void testConfigurablePhcGeoFence() {
        // PHC with custom 1000m radius (e.g., Rural/Hilly PHC)
        PHC customPhc = new PHC("Rural Hilly PHC", "Ooty", "PHC", 11.4102, 76.6950, 1000.0, null);
        Doctor customDoc = new Doctor("Dr. Shanmugam", "shan@phc.gov.in", "pass", "General", "DOCTOR", customPhc);

        Mockito.when(doctorRepository.findById(2L)).thenReturn(Optional.of(customDoc));

        // Attempt check-in at 700m away (lat 11.4160, lng 76.6950 is ~640m away)
        String result = attendanceService.checkIn(2L, 11.4160, 76.6950, 10.0);
        assertTrue(result.startsWith("Check-in successful!"));

        // PHC with narrow 200m radius (e.g. Dense Urban PHC)
        customPhc.setRadiusMeters(200.0);
        // Reset attendance check for clean test
        String failResult = attendanceService.checkIn(2L, 11.4160, 76.6950, 10.0);
        assertTrue(failResult.contains("Outside PHC location") && failResult.contains("Maximum allowed distance is 200m"));
    }

    @Test
    void testAdvancedAttendanceRulesLateCheckIn() {
        // Doctor checking in at 09:30 AM (Shift Start: 09:00 AM, Grace: 15 mins -> Cutoff: 09:15 AM)
        String result = attendanceService.checkIn(1L, 11.0168, 76.9558, 10.0, LocalTime.of(9, 30), LocalTime.of(9, 0), 15);
        assertTrue(result.contains("Marked LATE"));
        assertTrue(result.contains("09:15"));
    }

    @Test
    void testOfflineSynchronizationBatch() {
        Map<String, Object> offlineCheckIn = Map.of(
            "offlineId", "off_101",
            "eventType", "CHECK_IN",
            "latitude", 11.0168,
            "longitude", 76.9558,
            "accuracy", 10.0,
            "timestamp", "09:05:00"
        );

        List<Map<String, Object>> syncResults = attendanceService.syncOfflineRecords(1L, List.of(offlineCheckIn));
        assertFalse(syncResults.isEmpty());
        assertEquals("off_101", syncResults.get(0).get("offlineId"));
        assertEquals("SUCCESS", syncResults.get(0).get("status"));
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void testAnomalyDetectionEngine() {
        Mockito.when(doctorRepository.findAll()).thenReturn(List.of(sampleDoctor));

        AttendanceAuditLog spoofLog = new AttendanceAuditLog(LocalDateTime.now(), "CHECK_IN_ATTEMPT", 11.0168, 76.9558, 10.0, 50000.0, "FLAGGED_IMPOSSIBLE_SPEED", "Impossible travel speed: 300 km/h", sampleDoctor);
        Mockito.when(auditLogRepository.findByDoctorOrderByTimestampDesc(sampleDoctor)).thenReturn(List.of(spoofLog));

        Attendance breachAttendance = new Attendance(LocalDate.now(), LocalTime.of(9, 0), LocalTime.of(17, 0), "PRESENT", sampleDoctor);
        breachAttendance.setPresenceBreachCount(4);
        Mockito.when(attendanceRepository.findByDoctor(sampleDoctor)).thenReturn(List.of(breachAttendance));

        List<Map<String, Object>> anomalies = attendanceService.getAttendanceAnomalies();
        assertFalse(anomalies.isEmpty());
        assertTrue(anomalies.stream().anyMatch(a -> "SUSPECTED_GPS_SPOOFING".equals(a.get("anomalyType"))));
        assertTrue(anomalies.stream().anyMatch(a -> "HIGH_PRESENCE_BREACHES".equals(a.get("anomalyType"))));
    }

    @Test
    void testDeviceBindingEnforcement() {
        PHCRepository phcRepo = Mockito.mock(PHCRepository.class);
        com.ranjith.phcbackend.service.AuthService authService = new com.ranjith.phcbackend.service.AuthService(doctorRepository, phcRepo);

        Doctor doc = new Doctor("Dr. Kumar", "kumar@phc.gov.in", "pass123", "Physician", "DOCTOR", null);
        Mockito.when(doctorRepository.findByEmail("kumar@phc.gov.in")).thenReturn(Optional.of(doc));

        // Initial login from Device A -> Binds Device A
        Map<String, Object> firstLogin = authService.login("kumar@phc.gov.in", "pass123", "PHC_DEV_DEVICE_A");
        assertNotNull(firstLogin);
        assertFalse(firstLogin.containsKey("error"));
        assertEquals("PHC_DEV_DEVICE_A", doc.getRegisteredDeviceId());

        // Subsequent login from Device B with valid password -> Re-binds Device B seamlessly
        Map<String, Object> secondLogin = authService.login("kumar@phc.gov.in", "pass123", "PHC_DEV_DEVICE_UNAUTHORIZED_B");
        assertNotNull(secondLogin);
        assertFalse(secondLogin.containsKey("error"));
        assertEquals("PHC_DEV_DEVICE_UNAUTHORIZED_B", doc.getRegisteredDeviceId());
    }

    @Test
    void testLeaveManagementWorkflow() {
        LeaveRequestRepository leaveRepo = Mockito.mock(LeaveRequestRepository.class);
        LeaveService leaveService = new LeaveService(leaveRepo, doctorRepository);

        LeaveRequest sampleLeave = new LeaveRequest("CASUAL_LEAVE", LocalDate.now(), LocalDate.now().plusDays(2), "Medical conference", "PENDING", LocalDateTime.now(), sampleDoctor);
        sampleLeave.setId(10L);

        Mockito.when(leaveRepo.save(any(LeaveRequest.class))).thenAnswer(i -> {
            LeaveRequest req = i.getArgument(0);
            if (req.getId() == null) req.setId(10L);
            return req;
        });
        Mockito.when(leaveRepo.findById(10L)).thenReturn(Optional.of(sampleLeave));

        // 1. Doctor applies for leave
        Map<String, Object> applyRes = leaveService.applyLeave(1L, "CASUAL_LEAVE", LocalDate.now(), LocalDate.now().plusDays(2), "Medical conference");
        assertEquals("Leave application submitted successfully", applyRes.get("message"));

        // 2. Admin reviews and approves leave
        Map<String, Object> reviewRes = leaveService.reviewLeave(10L, "APPROVED");
        assertEquals("APPROVED", reviewRes.get("status"));

        // 3. Verify doctor is recognized as on approved leave
        Mockito.when(leaveRepo.findByDoctorAndStatus(sampleDoctor, "APPROVED")).thenReturn(List.of(sampleLeave));
        assertTrue(leaveService.isDoctorOnApprovedLeave(sampleDoctor, LocalDate.now()));

        // 4. Inject LeaveService into AttendanceService & verify absentee check exempts doctor on approved leave
        attendanceService.setLeaveService(leaveService);
        Mockito.when(doctorRepository.findAll()).thenReturn(List.of(sampleDoctor));
        Mockito.when(attendanceRepository.findByDoctorAndDate(sampleDoctor, LocalDate.now())).thenReturn(Optional.empty());

        int newlyFlagged = attendanceService.runAutomatedAbsenteeCheck();
        assertEquals(0, newlyFlagged); // Doctor on approved leave should NOT be flagged as absent alert

        List<Map<String, Object>> alerts = attendanceService.getAbsenteeAlerts();
        assertTrue(alerts.isEmpty()); // Absentee alerts list should skip doctors on approved leave
    }

    @Test
    void testFacialLivenessVerification() {
        // 1. Check-in with low liveness score (< 0.70) -> Rejects check-in
        String lowScoreResult = attendanceService.checkIn(1L, 11.0168, 76.9558, 10.0, 0.45, "data:image/jpeg;base64,mockLowLivenessData");
        assertTrue(lowScoreResult.contains("Facial liveness verification failed"));
        assertTrue(lowScoreResult.contains("0.45"));

        // 2. Check-in with valid liveness score (>= 0.70) -> Succeeds and persists score & photo proof
        String validScoreResult = attendanceService.checkIn(1L, 11.0168, 76.9558, 10.0, 0.98, "data:image/jpeg;base64,mockValidPhotoData");
        assertTrue(validScoreResult.startsWith("Check-in successful!"));
        verify(attendanceRepository).save(any(Attendance.class));
    }

    @Test
    void testSurveillanceNotificationFeed() {
        AttendanceAuditLog breachLog = new AttendanceAuditLog(LocalDateTime.now(), "PRESENCE_PING", 13.0827, 80.2707, 10.0, 500.0, "PRESENCE_BREACH_WARNING", "Presence breach outside PHC", sampleDoctor);
        AttendanceAuditLog spoofLog = new AttendanceAuditLog(LocalDateTime.now(), "CHECK_IN_ATTEMPT", 11.0168, 76.9558, 10.0, 50000.0, "FLAGGED_IMPOSSIBLE_SPEED", "Impossible travel speed detected", sampleDoctor);

        Mockito.when(auditLogRepository.findTop50ByOrderByTimestampDesc()).thenReturn(List.of(breachLog, spoofLog));

        List<Map<String, Object>> feed = attendanceService.getSurveillanceFeed();
        assertNotNull(feed);
        assertEquals(2, feed.size());

        Map<String, Object> firstItem = feed.get(0);
        assertEquals("WARNING", firstItem.get("severity"));
        assertEquals("Dr. Arunkumar", firstItem.get("doctorName"));

        Map<String, Object> secondItem = feed.get(1);
        assertEquals("CRITICAL", secondItem.get("severity"));
    }

    @Test
    void testPwaAssetConfiguration() {
        java.io.File manifestFile = new java.io.File("c:/Users/B Ranjith/Downloads/phc-doctor-attendance-system/frontend/manifest.json");
        java.io.File swFile = new java.io.File("c:/Users/B Ranjith/Downloads/phc-doctor-attendance-system/frontend/sw.js");

        assertTrue(manifestFile.exists(), "PWA manifest.json should exist");
        assertTrue(swFile.exists(), "PWA sw.js service worker should exist");
    }

    // ==========================================
    // SYSTEM AUDIT & HARDENING TEST SUITE
    // ==========================================

    @Test
    void testPreventDuplicateAttendanceRaceCondition() {
        // Mock scenario: doctor has not yet checked in according to read query
        Mockito.when(attendanceRepository.findByDoctorAndDate(sampleDoctor, LocalDate.now()))
                .thenReturn(Optional.empty());

        // But concurrent thread inserts first, triggering the unique database constraint
        Mockito.when(attendanceRepository.save(any(Attendance.class)))
                .thenThrow(new org.springframework.dao.DataIntegrityViolationException("Duplicate entry uk_doctor_attendance_date"));

        String result = attendanceService.checkIn(1L, 11.0168, 76.9558, 10.0);
        assertEquals("Already checked in today", result);
    }

    @Test
    void testAttendanceStatusTransitionsAndCheckoutValidation() {
        // 1. Cannot checkout without checkin
        Mockito.when(attendanceRepository.findByDoctorAndDate(sampleDoctor, LocalDate.now()))
                .thenReturn(Optional.empty());
        String noCheckIn = attendanceService.checkOut(1L);
        assertEquals("No check-in found for today", noCheckIn);

        // 2. Doctor marked ABSENT cannot checkout
        Attendance absentAtt = new Attendance(LocalDate.now(), null, null, "ABSENT", sampleDoctor);
        Mockito.when(attendanceRepository.findByDoctorAndDate(sampleDoctor, LocalDate.now()))
                .thenReturn(Optional.of(absentAtt));
        String absentCheckout = attendanceService.checkOut(1L);
        assertTrue(absentCheckout.contains("marked ABSENT"));

        // 3. Early checkout (< 4 hours worked) -> Transitions to COMPLETED_EARLY
        Attendance presentAtt = new Attendance(LocalDate.now(), LocalTime.now().minusHours(2), null, "PRESENT", sampleDoctor);
        Mockito.when(attendanceRepository.findByDoctorAndDate(sampleDoctor, LocalDate.now()))
                .thenReturn(Optional.of(presentAtt));
        String earlyCheckout = attendanceService.checkOut(1L);
        assertTrue(earlyCheckout.contains("Early Departure") || earlyCheckout.contains("Check-out successful"));
        assertEquals("COMPLETED_EARLY", presentAtt.getStatus());

        // 4. Repeated checkout attempt -> Rejected
        String repeatCheckout = attendanceService.checkOut(1L);
        assertEquals("Already checked out today", repeatCheckout);
    }

    @Test
    void testDashboardStatisticsAccuracyAgainstDatabase() {
        PHCRepository mockPhcRepo = Mockito.mock(PHCRepository.class);
        DoctorRepository mockDocRepo = Mockito.mock(DoctorRepository.class);
        AttendanceRepository mockAttRepo = Mockito.mock(AttendanceRepository.class);
        LeaveService mockLeaveService = Mockito.mock(LeaveService.class);

        com.ranjith.phcbackend.model.Division division = new com.ranjith.phcbackend.model.Division("Coimbatore North", "Coimbatore");
        division.setId(1L);

        PHC phc = new PHC("Thudiyalur PHC", "Coimbatore", "PHC", 11.0168, 76.9558, division);
        phc.setId(10L);

        Doctor doc1 = new Doctor("Dr. One (Late)", "doc1@phc.gov.in", "p1", "Gen", "DOCTOR", phc);
        doc1.setId(101L);
        Doctor doc2 = new Doctor("Doc Two (Early)", "doc2@phc.gov.in", "p2", "Gen", "DOCTOR", phc);
        doc2.setId(102L);
        Doctor doc3 = new Doctor("Doc Three (Leave)", "doc3@phc.gov.in", "p3", "Gen", "DOCTOR", phc);
        doc3.setId(103L);
        Doctor doc4 = new Doctor("Doc Four (Absent)", "doc4@phc.gov.in", "p4", "Gen", "DOCTOR", phc);
        doc4.setId(104L);

        List<Doctor> allDocs = List.of(doc1, doc2, doc3, doc4);
        Mockito.when(mockPhcRepo.findAll()).thenReturn(List.of(phc));
        Mockito.when(mockDocRepo.findAll()).thenReturn(allDocs);

        LocalDate today = LocalDate.now();

        // Doc 1 was LATE (should count as present/attended)
        Attendance att1 = new Attendance(today, LocalTime.of(9, 30), null, "LATE", doc1);
        Mockito.when(mockAttRepo.findByDoctorAndDate(doc1, today)).thenReturn(Optional.of(att1));

        // Doc 2 was COMPLETED_EARLY (should count as present/attended)
        Attendance att2 = new Attendance(today, LocalTime.of(9, 0), LocalTime.of(12, 0), "COMPLETED_EARLY", doc2);
        Mockito.when(mockAttRepo.findByDoctorAndDate(doc2, today)).thenReturn(Optional.of(att2));

        // Doc 3 is ON_LEAVE
        Attendance att3 = new Attendance(today, null, null, "ON_LEAVE", doc3);
        Mockito.when(mockAttRepo.findByDoctorAndDate(doc3, today)).thenReturn(Optional.of(att3));
        Mockito.when(mockLeaveService.isDoctorOnApprovedLeave(doc3, today)).thenReturn(true);

        // Doc 4 has no attendance (ABSENT)
        Mockito.when(mockAttRepo.findByDoctorAndDate(doc4, today)).thenReturn(Optional.empty());
        Mockito.when(mockLeaveService.isDoctorOnApprovedLeave(doc4, today)).thenReturn(false);

        com.ranjith.phcbackend.service.DashboardService dashboardService =
                new com.ranjith.phcbackend.service.DashboardService(mockPhcRepo, mockDocRepo, mockAttRepo, mockLeaveService);

        // 1. Verify Summary Metrics
        Map<String, Object> summary = dashboardService.getSummary(1L);
        assertEquals(1, summary.get("totalPhc"));
        assertEquals(4, summary.get("totalDoctors"));
        assertEquals(2L, summary.get("presentDoctors"), "Doctors with LATE and COMPLETED_EARLY must be counted as present");
        assertEquals(1L, summary.get("onLeaveDoctors"), "Doctors with approved leave must be counted as on leave");
        assertEquals(1L, summary.get("absentDoctors"), "Only unexcused absent doctors must be counted as absent");

        // 2. Verify PHC Overview
        List<Map<String, Object>> overview = dashboardService.getPhcOverview(1L);
        assertEquals(1, overview.size());
        assertEquals(2L, overview.get(0).get("present"));
        assertEquals(1L, overview.get(0).get("absent"));
        assertEquals("50.00", overview.get(0).get("percentage"));

        // 3. Verify Analytics Status Breakdown
        Map<String, Object> analytics = dashboardService.getAnalytics(1L);
        @SuppressWarnings("unchecked")
        Map<String, Object> breakdown = (Map<String, Object>) analytics.get("statusBreakdown");
        assertNotNull(breakdown);
        assertEquals(2L, breakdown.get("present"));
        assertEquals(1L, breakdown.get("late"), "Late count should be accurately reflected");
        assertEquals(1L, breakdown.get("onLeave"), "On leave count should match database records");
        assertEquals(1L, breakdown.get("absent"), "Absent count should match unexcused absences");
    }

    @Test
    void testSecurityUtilSessionAndRoleAuthorization() {
        // 1. Password hashing & BCrypt verification
        String rawPass = "SecretDoctorPass2026!";
        String hashed = com.ranjith.phcbackend.security.SecurityUtil.hashPassword(rawPass);
        assertNotEquals(rawPass, hashed);
        assertTrue(com.ranjith.phcbackend.security.SecurityUtil.verifyPassword(rawPass, hashed));
        assertFalse(com.ranjith.phcbackend.security.SecurityUtil.verifyPassword("WrongPass", hashed));

        // 2. Token creation and role validation
        String doctorToken = com.ranjith.phcbackend.security.SecurityUtil.createSession(55L, "Dr. User", "user@phc.gov.in", "DOCTOR");
        String adminToken = com.ranjith.phcbackend.security.SecurityUtil.createSession(1L, "DDHS Admin", "admin@phc.gov.in", "ADMIN");

        assertTrue(com.ranjith.phcbackend.security.SecurityUtil.isValidAdmin(adminToken));
        assertFalse(com.ranjith.phcbackend.security.SecurityUtil.isValidAdmin(doctorToken));
        assertFalse(com.ranjith.phcbackend.security.SecurityUtil.isValidAdmin("INVALID_TOKEN"));

        // 3. Resource-level authorization: Doctor can only access own ID; Admin can access any
        assertTrue(com.ranjith.phcbackend.security.SecurityUtil.isAuthorizedDoctor(doctorToken, 55L));
        assertFalse(com.ranjith.phcbackend.security.SecurityUtil.isAuthorizedDoctor(doctorToken, 99L));
        assertTrue(com.ranjith.phcbackend.security.SecurityUtil.isAuthorizedDoctor(adminToken, 99L));

        // 4. Session invalidation
        com.ranjith.phcbackend.security.SecurityUtil.invalidateSession(doctorToken);
        assertNull(com.ranjith.phcbackend.security.SecurityUtil.getSession(doctorToken));
    }

    @Test
    void testLeaveAuthorizationEnforcement() {
        LeaveRequestRepository leaveRepo = Mockito.mock(LeaveRequestRepository.class);
        LeaveService leaveService = new LeaveService(leaveRepo, doctorRepository);
        com.ranjith.phcbackend.controller.LeaveController leaveController =
                new com.ranjith.phcbackend.controller.LeaveController(leaveService);

        String doctorToken = com.ranjith.phcbackend.security.SecurityUtil.createSession(10L, "Doctor", "doc@phc.gov.in", "DOCTOR");
        String adminToken = com.ranjith.phcbackend.security.SecurityUtil.createSession(1L, "Admin", "admin@phc.gov.in", "ADMIN");

        // Doctor trying to view pending leaves -> 403 Forbidden
        org.springframework.http.ResponseEntity<?> doctorPending = leaveController.getPendingLeaves(doctorToken);
        assertEquals(403, doctorPending.getStatusCode().value());

        // Admin viewing pending leaves -> 200 OK
        org.springframework.http.ResponseEntity<?> adminPending = leaveController.getPendingLeaves(adminToken);
        assertEquals(200, adminPending.getStatusCode().value());

        // Doctor trying to approve leave -> 403 Forbidden
        org.springframework.http.ResponseEntity<?> doctorApprove = leaveController.reviewLeave(1L, Map.of("status", "APPROVED"), doctorToken);
        assertEquals(403, doctorApprove.getStatusCode().value());
    }

    @Test
    void testDatabaseConstraintsAndInvalidInputs() {
        LeaveRequestRepository leaveRepo = Mockito.mock(LeaveRequestRepository.class);
        LeaveService leaveService = new LeaveService(leaveRepo, doctorRepository);

        // 1. Invalid date sequence: end date before start date
        LocalDate start = LocalDate.now().plusDays(5);
        LocalDate end = LocalDate.now().plusDays(2);
        Map<String, Object> invalidDateRes = leaveService.applyLeave(1L, "CASUAL", start, end, "Reason");
        assertTrue(invalidDateRes.containsKey("error"));
        assertTrue(invalidDateRes.get("error").toString().contains("End date cannot be earlier than start date"));

        // 2. Missing doctor ID
        Map<String, Object> nullDocRes = leaveService.applyLeave(null, "CASUAL", LocalDate.now(), LocalDate.now().plusDays(1), "Reason");
        assertTrue(nullDocRes.containsKey("error"));

        // 3. Null coordinates for check-in
        String nullCoords = attendanceService.checkIn(1L, null, 76.9558, 10.0);
        assertTrue(nullCoords.contains("required"));

        // 4. Latitude outside [-90, +90]
        String invalidLat = attendanceService.checkIn(1L, -95.0, 76.9558, 10.0);
        assertTrue(invalidLat.contains("Invalid latitude"));

        // 5. Longitude outside [-180, +180]
        String invalidLng = attendanceService.checkIn(1L, 11.0, 195.0, 10.0);
        assertTrue(invalidLng.contains("Invalid longitude"));
    }
}
