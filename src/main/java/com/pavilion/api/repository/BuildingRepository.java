package com.pavilion.api.repository;

import com.pavilion.api.entity.Building;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BuildingRepository extends JpaRepository<Building, Long> {
    Optional<Building> findByNameIgnoreCase(String name);
}
