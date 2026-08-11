package com.pavilion.api.dto;

import com.pavilion.api.entity.AuditLog;
import com.pavilion.api.entity.Notice;
import com.pavilion.api.entity.Service;
import com.pavilion.api.entity.SocietyRule;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public class NoticesRulesServicesDtos {

    // ---- Notices ----

    public record NoticeResponse(
            Long id, String title, String content, String category, String priority,
            boolean pinned, String expiresAt, Instant createdAt) {
        public static NoticeResponse from(Notice notice) {
            return new NoticeResponse(
                    notice.getId(), notice.getTitle(), notice.getContent(), notice.getCategory(), notice.getPriority(),
                    notice.isPinned(), notice.getExpiresAt(), notice.getCreatedAt());
        }
    }

    public record NoticeRequest(
            @NotBlank(message = "Title is required") String title,
            @NotBlank(message = "Content is required") String content,
            @NotBlank(message = "Category is required")
            @Pattern(regexp = "^(general|maintenance|event|urgent)$", message = "category must be general, maintenance, event, or urgent")
            String category,
            @NotBlank(message = "Priority is required")
            @Pattern(regexp = "^(low|normal|high)$", message = "priority must be low, normal, or high")
            String priority,
            @NotNull(message = "Pinned is required") Boolean pinned,
            String expiresAt) {
    }

    // ---- Society Rules ----

    public record SocietyRuleResponse(Long id, String title, String description, boolean active) {
        public static SocietyRuleResponse from(SocietyRule rule) {
            return new SocietyRuleResponse(rule.getId(), rule.getTitle(), rule.getDescription(), rule.isActive());
        }
    }

    public record SocietyRuleRequest(
            @NotBlank(message = "Title is required") String title,
            @NotBlank(message = "Description is required") String description,
            @NotNull(message = "Active is required") Boolean active) {
    }

    // ---- Services ----

    public record ServiceResponse(Long id, String name, String category, String contactNumber, String notes) {
        public static ServiceResponse from(Service service) {
            return new ServiceResponse(service.getId(), service.getName(), service.getCategory(), service.getContactNumber(), service.getNotes());
        }
    }

    public record ServiceRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Category is required") String category,
            @NotBlank(message = "Contact number is required")
            @Pattern(regexp = ValidationPatterns.PHONE_10_DIGIT,
                    message = "Contact number must be exactly 10 digits, starting with 6-9")
            String contactNumber,
            String notes) {

        public ServiceRequest {
            if (contactNumber != null) {
                contactNumber = contactNumber.trim().replaceAll("[\\s-]", "");
            }
        }
    }

    // ---- Audit Logs ----

    public record AuditLogResponse(
            Long id, Long adminId, String adminName, String method, String path, Integer statusCode,
            String summary, Instant createdAt) {
        public static AuditLogResponse from(AuditLog log) {
            return new AuditLogResponse(
                    log.getId(), log.getAdminId(), log.getAdminName(), log.getMethod(), log.getPath(),
                    log.getStatusCode(), log.getSummary(), log.getCreatedAt());
        }
    }
}
