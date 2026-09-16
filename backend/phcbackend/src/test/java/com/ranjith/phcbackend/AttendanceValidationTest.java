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

        // Subsequent login from Unauthorized Device B -> Rejects login
        Map<String, Object> secondLogin = authService.login("kumar@phc.gov.in", "pass123", "PHC_DEV_DEVICE_UNAUTHORIZED_B");
        assertNotNull(secondLogin);
        assertTrue(secondLogin.containsKey("error"));
        assertTrue(secondLogin.get("error").toString().contains("Device binding restriction"));
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
}

