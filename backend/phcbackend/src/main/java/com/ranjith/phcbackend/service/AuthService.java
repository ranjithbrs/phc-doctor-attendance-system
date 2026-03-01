package com.ranjith.phcbackend.service;

import com.ranjith.phcbackend.model.Doctor;
import com.ranjith.phcbackend.repository.DoctorRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final DoctorRepository doctorRepository;

    public AuthService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public Doctor login(String email, String password) {

        Optional<Doctor> doctorOptional = doctorRepository.findByEmail(email);

        if (doctorOptional.isEmpty()) {
            return null;
        }

        Doctor doctor = doctorOptional.get();

        if (!doctor.getPassword().equals(password)) {
            return null;
        }

        return doctor;
    }
}