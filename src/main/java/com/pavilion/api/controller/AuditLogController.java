package com.pavilion.api.controller;

import com.pavilion.api.dto.NoticesRulesServicesDtos.AuditLogResponse;
import com.pavilion.api.repository.AuditLogRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;

    public AuditLogController(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @GetMapping
    public List<AuditLogResponse> listAuditLogs() {
        return auditLogRepository.findTop500ByOrderByCreatedAtDesc().stream().map(AuditLogResponse::from).toList();
    }
}
