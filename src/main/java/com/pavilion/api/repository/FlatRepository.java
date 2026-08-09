package com.pavilion.api.repository;

import com.pavilion.api.entity.Flat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FlatRepository extends JpaRepository<Flat, Long> {
    boolean existsByBuildingId(Long buildingId);

    Optional<Flat> findByResidentId(Long residentId);
}
