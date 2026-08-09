package com.pavilion.api.controller;

import com.pavilion.api.dto.TransactionsDtos.MaintenanceSettingsResponse;
import com.pavilion.api.dto.TransactionsDtos.UpdateMaintenanceSettingsRequest;
import com.pavilion.api.entity.MaintenanceSettings;
import com.pavilion.api.repository.MaintenanceSettingsRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-settings")
@PreAuthorize("hasRole('ADMIN')")
public class MaintenanceSettingsController {

    private final MaintenanceSettingsRepository maintenanceSettingsRepository;

    public MaintenanceSettingsController(MaintenanceSettingsRepository maintenanceSettingsRepository) {
        this.maintenanceSettingsRepository = maintenanceSettingsRepository;
    }

    private MaintenanceSettings getOrCreateSettings() {
        List<MaintenanceSettings> existing = maintenanceSettingsRepository.findAll();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return maintenanceSettingsRepository.save(new MaintenanceSettings());
    }

    @GetMapping
    public MaintenanceSettingsResponse getMaintenanceSettings() {
        return MaintenanceSettingsResponse.from(getOrCreateSettings());
    }

    @PutMapping
    public MaintenanceSettingsResponse updateMaintenanceSettings(@Valid @RequestBody UpdateMaintenanceSettingsRequest body) {
        MaintenanceSettings settings = getOrCreateSettings();
        settings.setDueDay(body.dueDay());
        settings.setLateFeePercent(body.lateFeePercent());
        settings.setOpeningBalanceNote(body.openingBalanceNote());
        return MaintenanceSettingsResponse.from(maintenanceSettingsRepository.save(settings));
    }
}
