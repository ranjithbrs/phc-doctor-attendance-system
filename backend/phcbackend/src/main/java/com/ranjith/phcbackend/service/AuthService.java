package com.ranjith.phcbackend.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.ranjith.phcbackend.model.Doctor;
import com.ranjith.phcbackend.model.PHC;
import com.ranjith.phcbackend.repository.DoctorRepository;
import com.ranjith.phcbackend.repository.PHCRepository;
import com.ranjith.phcbackend.security.SecurityUtil;

@Service
public class AuthService {

    private final DoctorRepository doctorRepository;
    private final PHCRepository phcRepository;

    public AuthService(DoctorRepository doctorRepository, PHCRepository phcRepository) {
        this.doctorRepository = doctorRepository;
        this.phcRepository = phcRepository;
    }

    public Map<String, Object> login(String email, String password) {
        return login(email, password, null);
    }

    public Map<String, Object> login(String email, String password, String deviceId) {
        Optional<Doctor> doctorOptional = doctorRepository.findByEmail(email);

        if (doctorOptional.isEmpty()) {
            return null;
        }

        Doctor doctor = doctorOptional.get();

        // Verify password with BCrypt (and legacy plaintext fallback)
        if (!SecurityUtil.verifyPassword(password, doctor.getPassword())) {
            return null;
        }

        // Device Binding Check & Dynamic Registration for Doctor role
        if ("DOCTOR".equalsIgnoreCase(doctor.getRole()) && deviceId != null && !deviceId.isBlank()) {
            if (doctor.getRegisteredDeviceId() == null || !doctor.getRegisteredDeviceId().equals(deviceId)) {
                doctor.setRegisteredDeviceId(deviceId);
                doctorRepository.save(doctor);
            }
        }

        // Generate session token
        String token = SecurityUtil.createSession(doctor.getId(), doctor.getName(), doctor.getEmail(), doctor.getRole());

        Map<String, Object> response = new HashMap<>();
        response.put("doctorId", doctor.getId());
        response.put("name", doctor.getName());
        response.put("role", doctor.getRole());
        response.put("registeredDeviceId", doctor.getRegisteredDeviceId());
        response.put("token", token);

        return response;
    }

    public Map<String, Object> resetDeviceBinding(String email) {
        Map<String, Object> response = new HashMap<>();
        Optional<Doctor> docOpt = doctorRepository.findByEmail(email);
        if (docOpt.isPresent()) {
            Doctor doc = docOpt.get();
            doc.setRegisteredDeviceId(null);
            doctorRepository.save(doc);
            response.put("message", "Device binding reset successfully");
        } else {
            response.put("error", "Doctor account not found");
        }
        return response;
    }

    public Map<String, Object> register(String name, String email, String password, String specialization, String role, Long phcId) {
        Map<String, Object> response = new HashMap<>();

        if (doctorRepository.findByEmail(email).isPresent()) {
            response.put("error", "Email is already registered");
            return response;
        }

        Optional<PHC> phcOpt = phcRepository.findById(phcId);
        if (phcOpt.isEmpty()) {
            response.put("error", "Invalid Primary Health Centre (PHC) selected");
            return response;
        }

        // Hash password with BCrypt
        String hashedPassword = SecurityUtil.hashPassword(password);

        Doctor newDoctor = new Doctor(name, email, hashedPassword, specialization, role, phcOpt.get());
        Doctor savedDoctor = doctorRepository.save(newDoctor);

        String token = SecurityUtil.createSession(savedDoctor.getId(), savedDoctor.getName(), savedDoctor.getEmail(), savedDoctor.getRole());

        response.put("message", "Registration successful");
        response.put("doctorId", savedDoctor.getId());
        response.put("name", savedDoctor.getName());
        response.put("role", savedDoctor.getRole());
        response.put("token", token);
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