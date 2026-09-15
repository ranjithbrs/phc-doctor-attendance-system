package com.ranjith.phcbackend.security;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class SecurityUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static final Map<String, SessionUser> activeSessions = new ConcurrentHashMap<>();

    public static class SessionUser {
        private final Long doctorId;
        private final String name;
        private final String email;
        private final String role;
        private final long createdAt;

        public SessionUser(Long doctorId, String name, String email, String role) {
            this.doctorId = doctorId;
            this.name = name;
            this.email = email;
            this.role = role;
            this.createdAt = System.currentTimeMillis();
        }

        public Long getDoctorId() { return doctorId; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public long getCreatedAt() { return createdAt; }
    }

    // ===== PASSWORD HASHING =====

    public static String hashPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.trim().isEmpty()) {
            return rawPassword;
        }
        return encoder.encode(rawPassword);
    }

    public static boolean verifyPassword(String rawPassword, String storedPassword) {
        if (rawPassword == null || storedPassword == null) {
            return false;
        }
        // Support BCrypt hashes
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return encoder.matches(rawPassword, storedPassword);
        }
        // Fallback: Plaintext match for pre-seeded legacy demo accounts
        return storedPassword.equals(rawPassword);
    }

    // ===== SESSION TOKEN MANAGEMENT =====

    public static String createSession(Long doctorId, String name, String email, String role) {
        String token = "PHC_SEC_TOKEN_" + UUID.randomUUID().toString().replace("-", "");
        activeSessions.put(token, new SessionUser(doctorId, name, email, role));
        return token;
    }

    public static SessionUser getSession(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        // Clean Bearer prefix if provided
        String cleanToken = token.replace("Bearer ", "").trim();
        return activeSessions.get(cleanToken);
    }

    public static boolean isValidAdmin(String token) {
        SessionUser user = getSession(token);
        return user != null && "ADMIN".equalsIgnoreCase(user.getRole());
    }

    public static boolean isAuthorizedDoctor(String token, Long targetDoctorId) {
        SessionUser user = getSession(token);
        if (user == null) {
            return false;
        }
        // Admins can manage any doctor, Doctors can only manage their own ID
        return "ADMIN".equalsIgnoreCase(user.getRole()) || user.getDoctorId().equals(targetDoctorId);
    }

    public static void invalidateSession(String token) {
        if (token != null) {
            String cleanToken = token.replace("Bearer ", "").trim();
            activeSessions.remove(cleanToken);
        }
    }
}
