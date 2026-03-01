package com.ranjith.phcbackend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

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

    @ManyToOne
    @JoinColumn(name = "phc_id", nullable = false)
    private PHC phc;

    // Default Constructor
    public Doctor() {
    }

    // Parameterized Constructor
    public Doctor(String name, String email, String password, String specialization, PHC phc) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.specialization = specialization;
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

    public void setPhc(PHC phc) {
        this.phc = phc;
    }
}