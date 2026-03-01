package com.ranjith.phcbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ranjith.phcbackend.model.Doctor;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    Optional<Doctor> findByEmail(String email);

}