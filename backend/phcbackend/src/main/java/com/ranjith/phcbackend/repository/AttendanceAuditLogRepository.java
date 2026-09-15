package com.ranjith.phcbackend.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.ranjith.phcbackend.model.AttendanceAuditLog;
import com.ranjith.phcbackend.model.Doctor;

public interface AttendanceAuditLogRepository extends JpaRepository<AttendanceAuditLog, Long> {
    List<AttendanceAuditLog> findByDoctorOrderByTimestampDesc(Doctor doctor);
    List<AttendanceAuditLog> findTop50ByOrderByTimestampDesc();
}
