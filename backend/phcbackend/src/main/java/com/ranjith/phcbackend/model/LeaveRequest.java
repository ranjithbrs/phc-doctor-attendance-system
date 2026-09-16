package com.ranjith.phcbackend.model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "leave_requests")
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String leaveType; // CASUAL_LEAVE, MEDICAL_LEAVE, DUTY_LEAVE

    private LocalDate startDate;

    private LocalDate endDate;

    @Column(length = 500)
    private String reason;

    private String status; // PENDING, APPROVED, REJECTED

    private LocalDateTime appliedAt;

    private LocalDateTime reviewedAt;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    public LeaveRequest() {
    }

    public LeaveRequest(String leaveType, LocalDate startDate, LocalDate endDate, String reason, String status, LocalDateTime appliedAt, Doctor doctor) {
        this.leaveType = leaveType;
        this.startDate = startDate;
        this.endDate = endDate;
        this.reason = reason;
        this.status = status;
        this.appliedAt = appliedAt;
        this.doctor = doctor;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public String getLeaveType() { return leaveType; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getReason() { return reason; }
    public String getStatus() { return status; }
    public LocalDateTime getAppliedAt() { return appliedAt; }
    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public Doctor getDoctor() { return doctor; }

    public void setId(Long id) { this.id = id; }
    public void setLeaveType(String leaveType) { this.leaveType = leaveType; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public void setReason(String reason) { this.reason = reason; }
    public void setStatus(String status) { this.status = status; }
    public void setAppliedAt(LocalDateTime appliedAt) { this.appliedAt = appliedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }
    public void setDoctor(Doctor doctor) { this.doctor = doctor; }
}
