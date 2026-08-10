package com.pavilion.api.repository;

import com.pavilion.api.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VehicleRepository extends JpaRepository<Vehicle, Long> {
    List<Vehicle> findAllByOrderByCreatedAtDesc();

    List<Vehicle> findByResidentIdOrderByCreatedAtDesc(Long residentId);
}
