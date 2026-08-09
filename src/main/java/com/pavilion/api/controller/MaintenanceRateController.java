package com.pavilion.api.controller;

import com.pavilion.api.dto.TransactionsDtos.MaintenanceRateRequest;
import com.pavilion.api.dto.TransactionsDtos.MaintenanceRateResponse;
import com.pavilion.api.entity.MaintenanceRate;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.MaintenanceRateRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/maintenance-rates")
@PreAuthorize("hasRole('ADMIN')")
public class MaintenanceRateController {

    private static final Set<String> FLAT_TYPES = Set.of("1bhk", "2bhk", "3bhk", "4bhk");

    private final MaintenanceRateRepository maintenanceRateRepository;

    public MaintenanceRateController(MaintenanceRateRepository maintenanceRateRepository) {
        this.maintenanceRateRepository = maintenanceRateRepository;
    }

    @GetMapping
    public List<MaintenanceRateResponse> listMaintenanceRates() {
        return maintenanceRateRepository.findAll().stream().map(MaintenanceRateResponse::from).toList();
    }

    @PutMapping("/{flatType}")
    public MaintenanceRateResponse updateMaintenanceRate(
            @PathVariable String flatType, @Valid @RequestBody MaintenanceRateRequest body) {
        if (!FLAT_TYPES.contains(flatType)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Unknown flat type");
        }

        MaintenanceRate rate = maintenanceRateRepository.findByFlatType(flatType).orElseGet(() -> {
            MaintenanceRate created = new MaintenanceRate();
            created.setFlatType(flatType);
            return created;
        });
        rate.setMonthlyAmountPaise(body.monthlyAmountPaise());
        return MaintenanceRateResponse.from(maintenanceRateRepository.save(rate));
    }
}
