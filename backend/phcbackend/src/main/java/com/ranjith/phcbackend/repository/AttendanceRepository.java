package com.ranjith.phcbackend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ranjith.phcbackend.model.Attendance;
import com.ranjith.phcbackend.model.Doctor;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {

    Optional<Attendance> findByDoctorAndDate(Doctor doctor, LocalDate date);

    List<Attendance> findByDoctor(Doctor doctor);

    List<Attendance> findByDoctorAndDateBetween(
            Doctor doctor,
            LocalDate startDate,
            LocalDate endDate
    );
}