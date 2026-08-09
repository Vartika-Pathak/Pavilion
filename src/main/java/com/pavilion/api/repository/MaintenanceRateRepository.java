package com.pavilion.api.repository;

import com.pavilion.api.entity.MaintenanceRate;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface MaintenanceRateRepository extends JpaRepository<MaintenanceRate, Long> {
    Optional<MaintenanceRate> findByFlatType(String flatType);
}
