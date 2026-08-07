package com.pavilion.api.dto;

import com.pavilion.api.entity.ResidentVerificationRequest;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public class AdminDtos {

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
