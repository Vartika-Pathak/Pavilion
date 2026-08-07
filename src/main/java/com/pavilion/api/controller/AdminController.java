package com.pavilion.api.controller;

import com.pavilion.api.dto.AdminDtos.UpdateVerificationRequestBody;
import com.pavilion.api.dto.AdminDtos.VerificationRequestSummary;
import com.pavilion.api.entity.ResidentVerificationRequest;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.ResidentVerificationRequestRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// Every endpoint here is admin-only — SecurityConfig's default anyRequest().authenticated()
// requires a session, and @PreAuthorize on top of that rejects anyone without the admin role.
@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final ResidentVerificationRequestRepository verificationRequestRepository;

    public AdminController(ResidentVerificationRequestRepository verificationRequestRepository) {
        this.verificationRequestRepository = verificationRequestRepository;
    }

    /** Pending requests first (oldest first within each group), so the queue reads top-to-bottom. */
    @GetMapping("/verification-requests")
    public List<VerificationRequestSummary> listVerificationRequests() {
        return verificationRequestRepository.findAllByOrderByStatusAscCreatedAtAsc().stream()
                .map(VerificationRequestSummary::from)
                .toList();
    }

    @PatchMapping("/verification-requests/{id}")
    public VerificationRequestSummary updateVerificationRequest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVerificationRequestBody body,
            @AuthenticationPrincipal User admin) {
        ResidentVerificationRequest request = verificationRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Verification request not found"));

        if (body.documentsVerified() != null) {
            request.setDocumentsVerified(body.documentsVerified());
        }
        if (body.paymentReceived() != null) {
            request.setPaymentReceived(body.paymentReceived());
        }

        if ("approve".equals(body.action())) {
            if (!request.isDocumentsVerified() || !request.isPaymentReceived()) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Both documents and payment must be verified before approving");
            }
            request.setStatus("approved");
            request.setReviewedBy(admin.getId());
            request.setReviewedAt(Instant.now());
        } else if ("reject".equals(body.action())) {
            request.setStatus("rejected");
            request.setReviewedBy(admin.getId());
            request.setReviewedAt(Instant.now());
        }

        return VerificationRequestSummary.from(verificationRequestRepository.save(request));
    }
}
