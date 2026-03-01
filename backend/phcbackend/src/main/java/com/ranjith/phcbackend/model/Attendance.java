package com.ranjith.phcbackend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDate date;

    private LocalTime checkInTime;

    private LocalTime checkOutTime;

    private String status; // PRESENT / ABSENT / LATE

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    // Default Constructor
    public Attendance() {
    }

    // Parameterized Constructor
    public Attendance(LocalDate date, LocalTime checkInTime,
                      LocalTime checkOutTime, String status, Doctor doctor) {
        this.date = date;
        this.checkInTime = checkInTime;
        this.checkOutTime = checkOutTime;
        this.status = status;
        this.doctor = doctor;
    }

    // Getters

    public Long getId() { return id; }
    public LocalDate getDate() { return date; }
    public LocalTime getCheckInTime() { return checkInTime; }
    public LocalTime getCheckOutTime() { return checkOutTime; }
    public String getStatus() { return status; }
    public Doctor getDoctor() { return doctor; }

    // Setters

    public void setDate(LocalDate date) { this.date = date; }
    public void setCheckInTime(LocalTime checkInTime) { this.checkInTime = checkInTime; }
    public void setCheckOutTime(LocalTime checkOutTime) { this.checkOutTime = checkOutTime; }
    public void setStatus(String status) { this.status = status; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
}