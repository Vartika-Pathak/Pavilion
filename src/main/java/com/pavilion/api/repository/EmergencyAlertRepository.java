package com.pavilion.api.repository;

import com.pavilion.api.entity.EmergencyAlert;
import com.pavilion.api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {
    Optional<EmergencyAlert> findByResidentAndStatus(User resident, String status);

    List<EmergencyAlert> findByStatusOrderByCreatedAtDesc(String status);
}
