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

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;

    private final DoctorRepository doctorRepository;

    public AttendanceService(

            AttendanceRepository attendanceRepository,

            DoctorRepository doctorRepository

    ) {

        this.attendanceRepository =
                attendanceRepository;

        this.doctorRepository =
                doctorRepository;

    }

    // ===== CHECK-IN WITH GEO-FENCING =====

    public String checkIn(Long doctorId, Double userLat, Double userLng) {

        if (doctorId == null) {
            return "Invalid doctor ID";
        }

        Optional<Doctor> doctorOptional =
                doctorRepository.findById(doctorId);

        if (doctorOptional.isEmpty()) {
            return "Doctor not found";
        }

        Doctor doctor = doctorOptional.get();
        LocalDate today = LocalDate.now();

        Optional<Attendance> existing =
                attendanceRepository.findByDoctorAndDate(doctor, today);

        if (existing.isPresent()) {
            Attendance attendance = existing.get();
            if (attendance.getStatus().equals("PRESENT") || attendance.getStatus().equals("COMPLETED")) {
                return "Already checked in today";
            }
        }

        Attendance attendance = existing.orElse(new Attendance());
        attendance.setDoctor(doctor);
        attendance.setDate(today);

        // Geo-fencing check against assigned PHC coordinates
        PHC phc = doctor.getPhc();
        boolean isWithinRange = true;
        double distanceMeters = 0.0;

        if (phc != null && phc.getLatitude() != null && phc.getLongitude() != null && userLat != null && userLng != null) {
            distanceMeters = calculateDistanceInMeters(userLat, userLng, phc.getLatitude(), phc.getLongitude());
            final double MAX_ALLOWED_DISTANCE_METERS = 500.0; // 500 meters radius

            if (distanceMeters > MAX_ALLOWED_DISTANCE_METERS) {
                isWithinRange = false;
            }
        }

        if (!isWithinRange) {
            attendance.setStatus("ABSENT");
            attendanceRepository.save(attendance);
            return String.format("Outside PHC location (%.0fm away from %s). Marked as ABSENT.", distanceMeters, phc != null ? phc.getName() : "PHC");
        }

        attendance.setCheckInTime(LocalTime.now());
        attendance.setStatus("PRESENT");
        attendanceRepository.save(attendance);

        return phc != null && phc.getLatitude() != null 
            ? String.format("Check-in successful! Distance to %s: %.0fm.", phc.getName(), distanceMeters)
            : "Check-in successful";
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

    public String checkIn(Long doctorId) {
        return checkIn(doctorId, null, null);
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