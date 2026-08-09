package com.pavilion.api.dto;

import com.pavilion.api.entity.Complaint;
import com.pavilion.api.entity.MaintenanceRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.List;

public class ResidentRequestsDtos {

    // ---- Complaints ----

    public record ComplaintResponse(
            Long id, String category, String description, String status, String resolutionNote,
            String residentName, String residentFlatNumber, Instant createdAt) {
        public static ComplaintResponse from(Complaint complaint) {
            return new ComplaintResponse(
                    complaint.getId(), complaint.getCategory(), complaint.getDescription(), complaint.getStatus(),
                    complaint.getResolutionNote(), complaint.getResidentName(), complaint.getResidentFlatNumber(),
                    complaint.getCreatedAt());
        }
    }

    public record ComplaintRequest(
            @NotBlank(message = "Category is required")
            @Pattern(regexp = "^(maintenance|security|noise|other)$", message = "category must be maintenance, security, noise, or other")
            String category,
            @NotBlank(message = "Description is required") String description) {
    }

    public record ComplaintStatusRequest(
            @NotBlank(message = "Status is required")
            @Pattern(regexp = "^(open|in_progress|resolved)$", message = "status must be open, in_progress, or resolved")
            String status,
            String resolutionNote) {
    }

    // ---- Maintenance Requests ----

    public record MaintenanceRequestResponse(
            Long id, String category, String description, List<String> photoUrls, String status,
            String residentName, String residentFlatNumber, Instant createdAt) {
        public static MaintenanceRequestResponse from(MaintenanceRequest request) {
            return new MaintenanceRequestResponse(
                    request.getId(), request.getCategory(), request.getDescription(), request.getPhotoUrls(),
                    request.getStatus(), request.getResidentName(), request.getResidentFlatNumber(), request.getCreatedAt());
        }
    }

    public record MaintenanceStatusRequest(
            @NotBlank(message = "Status is required")
            @Pattern(regexp = "^(open|in_progress|resolved)$", message = "status must be open, in_progress, or resolved")
            String status) {
    }
}
