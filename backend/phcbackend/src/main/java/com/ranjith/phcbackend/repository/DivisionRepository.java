package com.ranjith.phcbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ranjith.phcbackend.model.Division;

public interface DivisionRepository extends JpaRepository<Division, Long> {
}