package com.pavilion.api.dto;

import com.pavilion.api.entity.ResidentVerificationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class AdminDtos {

    /**
     * Guards never sign up — an admin creates their login here instead. The email pattern
     * enforces the @pavilion.com staff domain so guard accounts stay visually distinct from
     * residents' own (arbitrary) email addresses.
     */
    public record CreateGuardRequest(
            @NotBlank(message = "Name is required")
            @Pattern(regexp = "^[A-Za-z ]{2,100}$", message = "Name can only contain letters and spaces")
            String name,
            @NotBlank(message = "Email is required")
            @Pattern(regexp = "^[A-Za-z0-9._%+-]+@pavilion\\.com$",
                    message = "Guard accounts must use a @pavilion.com email address")
            String email,
            @NotBlank(message = "Password is required")
            @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
            String password) {
    }

    public record VerificationRequestSummary(
            Long id,
            String flatNumber,
            String name,
            boolean documentsVerified,
            boolean paymentReceived,
            String status,
            Instant createdAt,
            Instant reviewedAt) {

        public static VerificationRequestSummary from(ResidentVerificationRequest request) {
            return new VerificationRequestSummary(
                    request.getId(),
                    request.getFlatNumber(),
                    request.getName(),
                    request.isDocumentsVerified(),
                    request.isPaymentReceived(),
                    request.getStatus(),
                    request.getCreatedAt(),
                    request.getReviewedAt());
        }
    }

    /**
     * All fields optional — send only what's changing. {@code action}, when present, must be
     * "approve" or "reject"; approving is rejected server-side unless both documentsVerified and
     * paymentReceived end up true.
     */
    public record UpdateVerificationRequestBody(
            Boolean documentsVerified,
            Boolean paymentReceived,
            @Pattern(regexp = "^(approve|reject)$", message = "action must be \"approve\" or \"reject\"")
            String action) {
    }
}
