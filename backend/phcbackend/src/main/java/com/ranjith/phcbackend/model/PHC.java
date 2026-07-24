package com.ranjith.phcbackend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "phcs")
public class PHC {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String location;

    private String type; // PHC / Upgraded PHC / SubCentre

    private Double latitude;

    private Double longitude;

    @ManyToOne
    @JoinColumn(name = "division_id", nullable = false)
    private Division division;

    // Default Constructor
    public PHC() {
    }

    // Parameterized Constructor
    public PHC(String name, String location, String type, Double latitude, Double longitude, Division division) {
        this.name = name;
        this.location = location;
        this.type = type;
        this.latitude = latitude;
        this.longitude = longitude;
        this.division = division;
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLocation() {
        return location;
    }

    public String getType() {
        return type;
    }

    public Double getLatitude() {
        return latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public Division getDivision() {
        return division;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public void setDivision(Division division) {
        this.division = division;
    }
}