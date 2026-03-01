package com.ranjith.phcbackend.service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ranjith.phcbackend.model.Attendance;
import com.ranjith.phcbackend.model.Doctor;
import com.ranjith.phcbackend.repository.AttendanceRepository;
import com.ranjith.phcbackend.repository.DoctorRepository;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final DoctorRepository doctorRepository;

    public AttendanceService(AttendanceRepository attendanceRepository,
                             DoctorRepository doctorRepository) {
        this.attendanceRepository = attendanceRepository;
        this.doctorRepository = doctorRepository;
    }

    // =========================
    // ✅ CHECK-IN
    // =========================
    public String checkIn(Long doctorId) {

        if (doctorId == null) {
            return "Invalid doctor ID";
        }

        Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);

        if (doctorOptional.isEmpty()) {
            return "Doctor not found";
        }

        Doctor doctor = doctorOptional.get();
        LocalDate today = LocalDate.now();

        Optional<Attendance> existingAttendance =
                attendanceRepository.findByDoctorAndDate(doctor, today);

        if (existingAttendance.isPresent()) {
            return "Already checked in today";
        }

        Attendance attendance = new Attendance();
        attendance.setDoctor(doctor);
        attendance.setDate(today);
        attendance.setCheckInTime(LocalTime.now());
        attendance.setStatus("PRESENT");

        attendanceRepository.save(attendance);

        return "Check-in successful";
    }

    // =========================
    // ✅ CHECK-OUT
    // =========================
    public String checkOut(Long doctorId) {

        if (doctorId == null) {
            return "Invalid doctor ID";
        }

        Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);

        if (doctorOptional.isEmpty()) {
            return "Doctor not found";
        }

        Doctor doctor = doctorOptional.get();
        LocalDate today = LocalDate.now();

        Optional<Attendance> attendanceOptional =
                attendanceRepository.findByDoctorAndDate(doctor, today);

        if (attendanceOptional.isEmpty()) {
            return "Doctor has not checked in today";
        }

        Attendance attendance = attendanceOptional.get();

        if (attendance.getCheckOutTime() != null) {
            return "Already checked out today";
        }

        attendance.setCheckOutTime(LocalTime.now());
        attendance.setStatus("COMPLETED");

        attendanceRepository.save(attendance);

        return "Check-out successful";
    }

    // =========================
    // ✅ TODAY STATUS
    // =========================
    public String getTodayStatus(Long doctorId) {

        if (doctorId == null) {
            return "Invalid doctor ID";
        }

        Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);

        if (doctorOptional.isEmpty()) {
            return "Doctor not found";
        }

        Doctor doctor = doctorOptional.get();
        LocalDate today = LocalDate.now();

        Optional<Attendance> attendanceOptional =
                attendanceRepository.findByDoctorAndDate(doctor, today);

        if (attendanceOptional.isEmpty()) {
            return "Not checked in";
        }

        Attendance attendance = attendanceOptional.get();

        if (attendance.getCheckOutTime() == null) {
            return "Checked in";
        }

        return "Checked out";
    }

    // =========================
    // ✅ ATTENDANCE HISTORY
    // =========================
    public List<Attendance> getAttendanceHistory(Long doctorId,
                                                 LocalDate startDate,
                                                 LocalDate endDate) {

        if (doctorId == null || startDate == null || endDate == null) {
            return List.of();
        }

        Optional<Doctor> doctorOptional = doctorRepository.findById(doctorId);

        if (doctorOptional.isEmpty()) {
            return List.of();
        }

        Doctor doctor = doctorOptional.get();

        return attendanceRepository
                .findByDoctorAndDateBetween(doctor, startDate, endDate);
    }
}