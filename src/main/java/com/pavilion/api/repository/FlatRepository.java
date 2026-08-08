package com.pavilion.api.repository;

import com.pavilion.api.entity.Flat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlatRepository extends JpaRepository<Flat, Long> {
    boolean existsByBuildingId(Long buildingId);
}
