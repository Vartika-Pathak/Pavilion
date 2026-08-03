package com.pavilion.api.controller;

import com.pavilion.api.dto.EmergencyAlertDtos.AlertResponse;
import com.pavilion.api.entity.EmergencyAlert;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.EmergencyAlertRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// Every endpoint here requires authentication (see SecurityConfig's default
// anyRequest().authenticated()) — the injected User is never null.
@RestController
@RequestMapping("/api/emergency-alerts")
public class EmergencyAlertController {

    private final EmergencyAlertRepository alertRepository;

    public EmergencyAlertController(EmergencyAlertRepository alertRepository) {
        this.alertRepository = alertRepository;
    }

    /** Raising an alert is idempotent — if the resident already has one active, that's returned as-is. */
    @PostMapping
    public ResponseEntity<AlertResponse> raise(@AuthenticationPrincipal User resident) {
        EmergencyAlert alert = alertRepository.findByResidentAndStatus(resident, "active")
                .orElseGet(() -> {
                    EmergencyAlert created = new EmergencyAlert();
                    created.setResident(resident);
                    created.setStatus("active");
                    return alertRepository.save(created);
                });

        return ResponseEntity.status(HttpStatus.CREATED).body(AlertResponse.from(alert, resident));
    }

    @GetMapping("/mine")
    public ResponseEntity<AlertResponse> mine(@AuthenticationPrincipal User resident) {
        return alertRepository.findByResidentAndStatus(resident, "active")
                .map(alert -> ResponseEntity.ok(AlertResponse.from(alert, resident)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/active")
    public List<AlertResponse> active() {
        return alertRepository.findByStatusOrderByCreatedAtDesc("active").stream()
                .map(alert -> AlertResponse.from(alert, alert.getResident()))
                .toList();
    }

    @PostMapping("/{id}/resolve")
    public AlertResponse resolve(@PathVariable Long id, @AuthenticationPrincipal User user) {
        EmergencyAlert alert = alertRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Alert not found"));

        boolean isReportingResident = alert.getResident().getId().equals(user.getId());
        boolean isStaff = "guard".equals(user.getRole()) || "admin".equals(user.getRole());
        if (!isReportingResident && !isStaff) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "Only the reporting resident, a guard, or an admin can resolve this alert");
        }

        alert.setStatus("resolved");
        alert.setResolvedBy(user);
        alert.setResolvedAt(Instant.now());
        alert = alertRepository.save(alert);

        return AlertResponse.from(alert, alert.getResident());
    }
}
