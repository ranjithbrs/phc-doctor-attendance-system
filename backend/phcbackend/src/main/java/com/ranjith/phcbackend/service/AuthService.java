package com.ranjith.phcbackend.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ranjith.phcbackend.model.Doctor;
import com.ranjith.phcbackend.repository.DoctorRepository;

import com.ranjith.phcbackend.repository.PHCRepository;

@Service
public class AuthService {

    private final DoctorRepository doctorRepository;
    private final PHCRepository phcRepository;

    public AuthService(DoctorRepository doctorRepository, PHCRepository phcRepository) {
        this.doctorRepository = doctorRepository;
        this.phcRepository = phcRepository;
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

    public Map<String, Object> register(String name, String email, String password, String specialization, String role, Long phcId) {
        Map<String, Object> response = new HashMap<>();

        if (doctorRepository.findByEmail(email).isPresent()) {
            response.put("error", "Email is already registered");
            return response;
        }

        Optional<com.ranjith.phcbackend.model.PHC> phcOpt = phcRepository.findById(phcId);
        if (phcOpt.isEmpty()) {
            response.put("error", "Invalid Primary Health Centre (PHC) selected");
            return response;
        }

        Doctor newDoctor = new Doctor(name, email, password, specialization, role, phcOpt.get());
        Doctor savedDoctor = doctorRepository.save(newDoctor);

        response.put("message", "Registration successful");
        response.put("doctorId", savedDoctor.getId());
        response.put("name", savedDoctor.getName());
        response.put("role", savedDoctor.getRole());
        return response;
    }

    public java.util.List<Map<String, Object>> getAllPhcs() {
        return phcRepository.findAll().stream().map(phc -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", phc.getId());
            map.put("name", phc.getName());
            map.put("location", phc.getLocation());
            return map;
        }).collect(java.util.stream.Collectors.toList());
    }
}