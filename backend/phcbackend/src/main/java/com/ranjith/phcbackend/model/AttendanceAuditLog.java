package com.ranjith.phcbackend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendance_audit_logs")
public class AttendanceAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime timestamp;

    private String action; // CHECK_IN_ATTEMPT, CHECK_OUT_ATTEMPT

    private Double latitude;

    private Double longitude;

    private Double accuracy; // GPS Uncertainty in meters

    private Double calculatedDistanceMeters; // Distance to assigned PHC

    private String verificationResult; // VERIFIED_SUCCESS, REJECTED_OUTSIDE_RADIUS, REJECTED_POOR_ACCURACY, REJECTED_INVALID_COORDINATES

    @Column(length = 500)
    private String remarks;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    public AttendanceAuditLog() {
    }

    public AttendanceAuditLog(LocalDateTime timestamp, String action, Double latitude, Double longitude,
                              Double accuracy, Double calculatedDistanceMeters, String verificationResult,
                              String remarks, Doctor doctor) {
        this.timestamp = timestamp;
        this.action = action;
        this.latitude = latitude;
        this.longitude = longitude;
        this.accuracy = accuracy;
        this.calculatedDistanceMeters = calculatedDistanceMeters;
        this.verificationResult = verificationResult;
        this.remarks = remarks;
        this.doctor = doctor;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public String getAction() { return action; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public Double getAccuracy() { return accuracy; }
    public Double getCalculatedDistanceMeters() { return calculatedDistanceMeters; }
    public String getVerificationResult() { return verificationResult; }
    public String getRemarks() { return remarks; }
    public Doctor getDoctor() { return doctor; }

    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public void setAction(String action) { this.action = action; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setAccuracy(Double accuracy) { this.accuracy = accuracy; }
    public void setCalculatedDistanceMeters(Double calculatedDistanceMeters) { this.calculatedDistanceMeters = calculatedDistanceMeters; }
    public void setVerificationResult(String verificationResult) { this.verificationResult = verificationResult; }
    public void setRemarks(String remarks) { this.remarks = remarks; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
}
