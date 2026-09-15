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
import com.ranjith.phcbackend.model.PHC;
import com.ranjith.phcbackend.repository.AttendanceAuditLogRepository;
import com.ranjith.phcbackend.repository.AttendanceRepository;
import com.ranjith.phcbackend.repository.DoctorRepository;
import com.ranjith.phcbackend.service.AttendanceService;

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
}
