package com.pavilion.api.controller;

import com.pavilion.api.dto.AdminDtos.CreateGuardRequest;
import com.pavilion.api.dto.AdminDtos.UpdateVerificationRequestBody;
import com.pavilion.api.dto.AdminDtos.VerificationRequestSummary;
import com.pavilion.api.dto.AuthDtos.AuthUserResponse;
import com.pavilion.api.entity.ResidentVerificationRequest;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.ResidentVerificationRequestRepository;
import com.pavilion.api.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(
            ResidentVerificationRequestRepository verificationRequestRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.verificationRequestRepository = verificationRequestRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Guards don't sign up — an admin creates the account here and hands the credentials to the
     * guard directly. flatNumber is set to a fixed placeholder since guards aren't tied to a flat.
     */
    @PostMapping("/guards")
    public ResponseEntity<AuthUserResponse> createGuard(@Valid @RequestBody CreateGuardRequest body) {
        if (userRepository.findByEmail(body.email()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User guard = new User();
        guard.setName(body.name());
        guard.setEmail(body.email());
        guard.setPasswordHash(passwordEncoder.encode(body.password()));
        guard.setFlatNumber("N/A");
        guard.setRole("guard");
        guard = userRepository.save(guard);

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthUserResponse.from(guard));
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
