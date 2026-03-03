package com.ranjith.phcbackend.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ranjith.phcbackend.model.Doctor;
import com.ranjith.phcbackend.repository.DoctorRepository;

@Service
public class AuthService {

    private final DoctorRepository doctorRepository;

    public AuthService(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    public Map<String, Object> login(String email, String password) {

        Optional<Doctor> doctorOptional = doctorRepository.findByEmail(email);

        if (doctorOptional.isEmpty()) {
            return null;
        }

        Doctor doctor = doctorOptional.get();

        if (!doctor.getPassword().equals(password)) {
            return null;
        }

        // ✅ Return structured response
        Map<String, Object> response = new HashMap<>();
        response.put("doctorId", doctor.getId());
        response.put("name", doctor.getName());
        response.put("role", doctor.getRole());

        return response;
    }
}