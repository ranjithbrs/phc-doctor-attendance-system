package com.ranjith.phcbackend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.ranjith.phcbackend.model.PHC;

public interface PHCRepository extends JpaRepository<PHC, Long> {
}