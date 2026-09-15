package com.ranjith.phcbackend;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ranjith.phcbackend.model.Doctor;
import com.ranjith.phcbackend.model.PHC;
import com.ranjith.phcbackend.repository.AttendanceRepository;
import com.ranjith.phcbackend.repository.DoctorRepository;
import com.ranjith.phcbackend.service.AttendanceService;

class AttendanceValidationTest {

    private AttendanceRepository attendanceRepository;
    private DoctorRepository doctorRepository;
    private AttendanceService attendanceService;
    private Doctor sampleDoctor;

    @BeforeEach
    void setUp() {
        attendanceRepository = Mockito.mock(AttendanceRepository.class);
        doctorRepository = Mockito.mock(DoctorRepository.class);
        attendanceService = new AttendanceService(attendanceRepository, doctorRepository);

        PHC phc = new PHC("Thudiyalur PHC", "Coimbatore", "PHC", 11.0168, 76.9558, null);
        sampleDoctor = new Doctor("Dr. Arunkumar", "doctor@phc.gov.in", "doc123", "Physician", "DOCTOR", phc);

        Mockito.when(doctorRepository.findById(1L)).thenReturn(Optional.of(sampleDoctor));
    }

    @Test
    void testValidCheckInWithinGeoFence() {
        // User at PHC coordinates (11.0168, 76.9558), high accuracy (10m)
        String result = attendanceService.checkIn(1L, 11.0168, 76.9558, 10.0);
        assertTrue(result.startsWith("Check-in successful!"));
    }

    @Test
    void testCheckInOutsideGeoFence() {
        // User in Chennai (13.0827, 80.2707), far away from Coimbatore PHC
        String result = attendanceService.checkIn(1L, 13.0827, 80.2707, 10.0);
        assertTrue(result.contains("Outside PHC location") && result.contains("ABSENT"));
    }

    @Test
    void testCheckInMissingCoordinates() {
        String result = attendanceService.checkIn(1L, null, null, 10.0);
        assertEquals("GPS location coordinates (latitude and longitude) are required for check-in.", result);
    }

    @Test
    void testCheckInInvalidLatitude() {
        String result = attendanceService.checkIn(1L, 100.0, 76.9558, 10.0);
        assertEquals("Invalid latitude value. Must be between -90 and +90 degrees.", result);
    }

    @Test
    void testCheckInInvalidLongitude() {
        String result = attendanceService.checkIn(1L, 11.0168, -200.0, 10.0);
        assertEquals("Invalid longitude value. Must be between -180 and +180 degrees.", result);
    }

    @Test
    void testCheckInPoorAccuracyThreshold() {
        // Accuracy 350m (> 200m threshold)
        String result = attendanceService.checkIn(1L, 11.0168, 76.9558, 350.0);
        assertTrue(result.contains("GPS accuracy is insufficient"));
    }
}
