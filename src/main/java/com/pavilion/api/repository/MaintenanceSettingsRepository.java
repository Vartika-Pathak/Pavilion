package com.pavilion.api.repository;

import com.pavilion.api.entity.MaintenanceSettings;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MaintenanceSettingsRepository extends JpaRepository<MaintenanceSettings, Long> {
}
