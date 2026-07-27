package com.pavilion.api.dto;

import com.pavilion.api.entity.EmergencyAlert;
import com.pavilion.api.entity.User;
import java.time.Instant;

public class EmergencyAlertDtos {

    public record AlertResponse(
            Long id,
            String status,
            String residentName,
            String residentFlatNumber,
            Instant createdAt,
            Instant resolvedAt) {

        public static AlertResponse from(EmergencyAlert alert, User resident) {
            return new AlertResponse(
                    alert.getId(),
                    alert.getStatus(),
                    resident.getName(),
                    resident.getFlatNumber(),
                    alert.getCreatedAt(),
                    alert.getResolvedAt());
        }
    }
}
