package com.ranjith.phcbackend.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ranjith.phcbackend.service.AuthService;

@RestController
@RequestMapping("/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {

        String email = request.get("email");
        String password = request.get("password");

        Map<String, Object> loginResponse = authService.login(email, password);

        if (loginResponse == null) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "Invalid email or password");
            return ResponseEntity.status(401).body(error);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Login successful");
        response.put("doctorId", loginResponse.get("doctorId"));
        response.put("name", loginResponse.get("name"));
        response.put("role", loginResponse.get("role"));

        return ResponseEntity.ok(response);
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, Object> request) {
        String name = (String) request.get("name");
        String email = (String) request.get("email");
        String password = (String) request.get("password");
        String specialization = (String) request.get("specialization");
        String role = (String) request.getOrDefault("role", "DOCTOR");
        if (request.get("phcId") == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "PHC selection is required"));
        }
        Long phcId;
        try {
            phcId = Long.valueOf(request.get("phcId").toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid PHC ID format"));
        }

        Map<String, Object> registerResponse = authService.register(name, email, password, specialization, role, phcId);

        if (registerResponse.containsKey("error")) {
            return ResponseEntity.badRequest().body(registerResponse);
        }

        return ResponseEntity.ok(registerResponse);
    }

    @org.springframework.web.bind.annotation.GetMapping("/phcs")
    public ResponseEntity<?> getPhcs() {
        return ResponseEntity.ok(authService.getAllPhcs());
    }
}