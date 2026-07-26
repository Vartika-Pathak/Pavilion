package com.pavilion.api.controller;

import com.pavilion.api.dto.VisitDtos.ConfirmVisitRequest;
import com.pavilion.api.dto.VisitDtos.CreateVisitRequest;
import com.pavilion.api.dto.VisitDtos.DecideVisitRequest;
import com.pavilion.api.dto.VisitDtos.LookupVisitRequest;
import com.pavilion.api.dto.VisitDtos.VisitLookupResult;
import com.pavilion.api.dto.VisitDtos.VisitResponse;
import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Visit;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.VisitRepository;
import com.pavilion.api.security.CurrentUserResolver;
import com.pavilion.api.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/visits")
public class VisitController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final VisitRepository visitRepository;
    private final CurrentUserResolver currentUserResolver;
    private final EmailService emailService;

    public VisitController(
            VisitRepository visitRepository,
            CurrentUserResolver currentUserResolver,
            EmailService emailService) {
        this.visitRepository = visitRepository;
        this.currentUserResolver = currentUserResolver;
        this.emailService = emailService;
    }

    @PostMapping
    public VisitResponse create(@Valid @RequestBody CreateVisitRequest body, HttpServletRequest request) {
        User resident = requireUser(request);

        boolean hasEmail = body.visitorEmail() != null && !body.visitorEmail().isBlank();
        String otpCode = generateOtpCode();

        Visit visit = new Visit();
        visit.setResident(resident);
        visit.setVisitType(body.visitType());
        visit.setVisitorName(body.visitorName());
        visit.setVisitorPhone(body.visitorPhone());
        visit.setVisitorEmail(body.visitorEmail());
        visit.setOtpCode(otpCode);
        // With an email on file, the resident has to enter the OTP we send the visitor before the
        // visit is usable at the gate — that's what proves they're really in touch with that visitor.
        // Without an email there's nothing to verify, so fall back to the old immediate flow.
        visit.setStatus(hasEmail ? "awaiting_verification" : "pending");
        visit.setExpiresAt(Instant.now().plus(4, ChronoUnit.HOURS));
        visit = visitRepository.save(visit);

        if (hasEmail) {
            emailService.sendVisitOtp(body.visitorEmail(), body.visitorName(), otpCode, body.visitType());
        }

        return VisitResponse.from(visit);
    }

    @PostMapping("/{id}/confirm")
    public VisitResponse confirm(
            @PathVariable Long id, @Valid @RequestBody ConfirmVisitRequest body, HttpServletRequest request) {
        User resident = requireUser(request);

        Visit visit = visitRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Visit not found"));

        if (!visit.getResident().getId().equals(resident.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This isn't your visit");
        }
        if (!"awaiting_verification".equals(visit.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This visit isn't awaiting verification");
        }
        if (visit.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "This code has expired — please log the visitor again");
        }
        if (!visit.getOtpCode().equals(body.otpCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Incorrect code — please check the email and try again");
        }

        visit.setStatus("pending");
        visit = visitRepository.save(visit);

        return VisitResponse.from(visit);
    }

    @GetMapping("/mine")
    public List<VisitResponse> mine(HttpServletRequest request) {
        User resident = requireUser(request);
        return visitRepository.findByResidentOrderByCreatedAtDesc(resident).stream()
                .map(VisitResponse::from)
                .toList();
    }

    @PostMapping("/lookup")
    public VisitLookupResult lookup(@Valid @RequestBody LookupVisitRequest body, HttpServletRequest request) {
        requireGuardOrAdmin(request);

        Visit visit = visitRepository.findByOtpCodeAndStatus(body.otpCode(), "pending")
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No pending visit with that OTP"));

        if (visit.getExpiresAt().isBefore(Instant.now())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "This OTP has expired");
        }

        return VisitLookupResult.from(visit, visit.getResident());
    }

    @PostMapping("/{id}/decide")
    public VisitResponse decide(
            @PathVariable Long id, @Valid @RequestBody DecideVisitRequest body, HttpServletRequest request) {
        User actor = requireGuardOrAdmin(request);

        Visit visit = visitRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Visit not found"));

        if (!"pending".equals(visit.getStatus())) {
            throw new ApiException(HttpStatus.CONFLICT, "This visit has already been decided");
        }

        visit.setStatus(body.approve() ? "approved" : "denied");
        visit.setApprovedBy(actor);
        visit.setRespondedAt(Instant.now());
        visit = visitRepository.save(visit);

        return VisitResponse.from(visit);
    }

    private static String generateOtpCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    private User requireUser(HttpServletRequest request) {
        return currentUserResolver.resolve(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not signed in"));
    }

    private User requireGuardOrAdmin(HttpServletRequest request) {
        User user = requireUser(request);
        if (!"guard".equals(user.getRole()) && !"admin".equals(user.getRole())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Only guards or admins can do this");
        }
        return user;
    }
}
