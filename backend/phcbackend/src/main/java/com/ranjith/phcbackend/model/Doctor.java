package com.ranjith.phcbackend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    private String password;

    private String specialization;

    private String role;   // ✅ ADD THIS

    @ManyToOne
    @JoinColumn(name = "phc_id", nullable = false)
    private PHC phc;

    // Default Constructor
    public Doctor() {
    }

    // Parameterized Constructor
    public Doctor(String name, String email, String password, String specialization, String role, PHC phc) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.specialization = specialization;
        this.role = role;
        this.phc = phc;
    }

    // Getters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getSpecialization() {
        return specialization;
    }

    public String getRole() {     // ✅ ADD THIS
        return role;
    }

    public PHC getPhc() {
        return phc;
    }

    // Setters

    public void setName(String name) {
        this.name = name;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setSpecialization(String specialization) {
        this.specialization = specialization;
    }

    public void setRole(String role) {   // ✅ ADD THIS
        this.role = role;
    }

    public void setPhc(PHC phc) {
        this.phc = phc;
    }
}