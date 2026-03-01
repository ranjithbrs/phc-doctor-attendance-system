package com.ranjith.phcbackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "divisions")
public class Division {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String districtName;

    // Default Constructor
    public Division() {
    }

    // Parameterized Constructor
    public Division(String name, String districtName) {
        this.name = name;
        this.districtName = districtName;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDistrictName() {
        return districtName;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setDistrictName(String districtName) {
        this.districtName = districtName;
    }
}