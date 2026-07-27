package com.pavilion.api.controller;

import com.pavilion.api.dto.EmergencyAlertDtos.AlertResponse;
import com.pavilion.api.entity.EmergencyAlert;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.EmergencyAlertRepository;
import com.pavilion.api.security.CurrentUserResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/emergency-alerts")
public class EmergencyAlertController {

    private final EmergencyAlertRepository alertRepository;
    private final CurrentUserResolver currentUserResolver;

    public EmergencyAlertController(
            EmergencyAlertRepository alertRepository, CurrentUserResolver currentUserResolver) {
        this.alertRepository = alertRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /** Raising an alert is idempotent — if the resident already has one active, that's returned as-is. */
    @PostMapping
    public ResponseEntity<AlertResponse> raise(HttpServletRequest request) {
        User resident = requireUser(request);

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
    public ResponseEntity<AlertResponse> mine(HttpServletRequest request) {
        User resident = requireUser(request);

        return alertRepository.findByResidentAndStatus(resident, "active")
                .map(alert -> ResponseEntity.ok(AlertResponse.from(alert, resident)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/active")
    public List<AlertResponse> active(HttpServletRequest request) {
        requireUser(request);

        return alertRepository.findByStatusOrderByCreatedAtDesc("active").stream()
                .map(alert -> AlertResponse.from(alert, alert.getResident()))
                .toList();
    }

    @PostMapping("/{id}/resolve")
    public AlertResponse resolve(@PathVariable Long id, HttpServletRequest request) {
        User user = requireUser(request);

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

    private User requireUser(HttpServletRequest request) {
        return currentUserResolver.resolve(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not signed in"));
    }
}
