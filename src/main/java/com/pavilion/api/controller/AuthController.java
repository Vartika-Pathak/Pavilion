package com.pavilion.api.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pavilion.api.dto.AuthDtos.AuthUserResponse;
import com.pavilion.api.dto.AuthDtos.FamilyMemberInput;
import com.pavilion.api.dto.AuthDtos.LoginRequest;
import com.pavilion.api.dto.AuthDtos.SignupPendingResponse;
import com.pavilion.api.dto.AuthDtos.SignupRequest;
import com.pavilion.api.dto.AuthDtos.SubmitVerificationRequest;
import com.pavilion.api.dto.AuthDtos.VerificationStatusResponse;
import com.pavilion.api.dto.AuthDtos.VerifySignupOtpRequest;
import com.pavilion.api.entity.FamilyMember;
import com.pavilion.api.entity.PendingSignup;
import com.pavilion.api.entity.ResidentVerificationRequest;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.FamilyMemberRepository;
import com.pavilion.api.repository.PendingSignupRepository;
import com.pavilion.api.repository.ResidentVerificationRequestRepository;
import com.pavilion.api.repository.UserRepository;
import com.pavilion.api.security.JwtAuthenticationFilter;
import com.pavilion.api.security.JwtService;
import com.pavilion.api.security.RecaptchaService;
import com.pavilion.api.service.EmailService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PendingSignupRepository pendingSignupRepository;
    private final ResidentVerificationRequestRepository verificationRequestRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RecaptchaService recaptchaService;
    private final EmailService emailService;
    private final ObjectMapper objectMapper;

    public AuthController(
            UserRepository userRepository,
            PendingSignupRepository pendingSignupRepository,
            ResidentVerificationRequestRepository verificationRequestRepository,
            FamilyMemberRepository familyMemberRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            RecaptchaService recaptchaService,
            EmailService emailService,
            ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.pendingSignupRepository = pendingSignupRepository;
        this.verificationRequestRepository = verificationRequestRepository;
        this.familyMemberRepository = familyMemberRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.recaptchaService = recaptchaService;
        this.emailService = emailService;
        this.objectMapper = objectMapper;
    }

    /**
     * A first-time resident's entry point into signup — submits a name + flat number for an admin
     * to review (see AdminController). Idempotent: re-submitting the same flat+name returns the
     * existing request rather than creating a duplicate, so the frontend can call this every time
     * the resident lands on the "confirm your residency" step.
     */
    @PostMapping("/verification-requests")
    public ResponseEntity<VerificationStatusResponse> submitVerificationRequest(
            @Valid @RequestBody SubmitVerificationRequest body) {
        ResidentVerificationRequest request = verificationRequestRepository
                .findByFlatNumberIgnoreCaseAndNameIgnoreCase(body.flatNumber(), body.name())
                .orElseGet(() -> {
                    ResidentVerificationRequest created = new ResidentVerificationRequest();
                    created.setFlatNumber(body.flatNumber());
                    created.setName(body.name());
                    return verificationRequestRepository.save(created);
                });

        return ResponseEntity.ok(verificationStatusResponse(request));
    }

    /**
     * Polled by the resident's "under review" screen. Always returns 200 (status="not_found" if
     * nothing matches) rather than 404, so the frontend can tell "no request yet" apart from "this
     * backend doesn't have the endpoint at all" (the Node backend, which skips this step entirely).
     */
    @GetMapping("/verification-requests/status")
    public ResponseEntity<VerificationStatusResponse> verificationStatus(
            @RequestParam String flatNumber, @RequestParam String name) {
        return ResponseEntity.ok(verificationRequestRepository
                .findByFlatNumberIgnoreCaseAndNameIgnoreCase(flatNumber, name)
                .map(this::verificationStatusResponse)
                .orElseGet(() -> new VerificationStatusResponse("not_found", "No verification request found")));
    }

    private VerificationStatusResponse verificationStatusResponse(ResidentVerificationRequest request) {
        String message = switch (request.getStatus()) {
            case "approved" -> "Verified";
            case "rejected" -> "Your request was declined — please check with the committee.";
            default -> "Your request is under review by the committee.";
        };
        return new VerificationStatusResponse(request.getStatus(), message);
    }

    /** Stages the account and emails an OTP — the real account isn't created until /signup/verify. */
    @PostMapping("/signup")
    public ResponseEntity<SignupPendingResponse> signup(@Valid @RequestBody SignupRequest body) {
        requireValidCaptcha(body.captchaToken());

        if (userRepository.findByEmail(body.email()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        // Re-submitting (e.g. after a lost/expired code) replaces any earlier pending signup.
        pendingSignupRepository.findByEmail(body.email()).ifPresent(pendingSignupRepository::delete);

        PendingSignup pending = new PendingSignup();
        pending.setName(body.name());
        pending.setEmail(body.email());
        pending.setFlatNumber(body.flatNumber());
        pending.setPasswordHash(passwordEncoder.encode(body.password()));
        pending.setOtpCode(generateOtpCode());
        pending.setExpiresAt(Instant.now().plus(10, ChronoUnit.MINUTES));
        if (body.familyMembers() != null && !body.familyMembers().isEmpty()) {
            pending.setFamilyMembersJson(writeFamilyMembersJson(body.familyMembers()));
        }
        pending = pendingSignupRepository.save(pending);

        emailService.sendSignupOtp(pending.getEmail(), pending.getName(), pending.getOtpCode());

        return ResponseEntity.ok(SignupPendingResponse.from(pending));
    }

    @PostMapping("/signup/verify")
    public ResponseEntity<AuthUserResponse> verifySignupOtp(@Valid @RequestBody VerifySignupOtpRequest body) {
        PendingSignup pending = pendingSignupRepository.findById(body.pendingSignupId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No signup is pending — please sign up again"));

        if (pending.getExpiresAt().isBefore(Instant.now())) {
            pendingSignupRepository.delete(pending);
            throw new ApiException(HttpStatus.BAD_REQUEST, "This code has expired — please sign up again");
        }

        if (!pending.getOtpCode().equals(body.otpCode())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Incorrect code — please try again");
        }

        if (userRepository.findByEmail(pending.getEmail()).isPresent()) {
            pendingSignupRepository.delete(pending);
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = new User();
        user.setName(pending.getName());
        user.setEmail(pending.getEmail());
        user.setFlatNumber(pending.getFlatNumber());
        user.setPasswordHash(pending.getPasswordHash());
        user = userRepository.save(user);

        if (pending.getFamilyMembersJson() != null) {
            for (FamilyMemberInput member : readFamilyMembersJson(pending.getFamilyMembersJson())) {
                FamilyMember familyMember = new FamilyMember();
                familyMember.setUserId(user.getId());
                familyMember.setName(member.name());
                familyMember.setRelation(member.relation());
                familyMember.setAge(member.age());
                familyMemberRepository.save(familyMember);
            }
        }

        pendingSignupRepository.delete(pending);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, sessionCookie(user.getId()).toString())
                .body(AuthUserResponse.from(user));
    }

    private static String generateOtpCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }

    private String writeFamilyMembersJson(List<FamilyMemberInput> familyMembers) {
        try {
            return objectMapper.writeValueAsString(familyMembers);
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Couldn't process family member details");
        }
    }

    private List<FamilyMemberInput> readFamilyMembersJson(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<FamilyMemberInput>>() {
            });
        } catch (Exception e) {
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Couldn't process family member details");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthUserResponse> login(@Valid @RequestBody LoginRequest body) {
        requireValidCaptcha(body.captchaToken());

        User user = userRepository.findByEmail(body.email())
                .filter(u -> passwordEncoder.matches(body.password(), u.getPasswordHash()))
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, sessionCookie(user.getId()).toString())
                .body(AuthUserResponse.from(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        ResponseCookie cleared = ResponseCookie.from(JwtAuthenticationFilter.SESSION_COOKIE, "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cleared.toString()).build();
    }

    // Never null here — this endpoint requires authentication (see SecurityConfig),
    // so Spring Security already rejected the request with 401 if it weren't.
    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(AuthUserResponse.from(user));
    }

    private void requireValidCaptcha(String token) {
        if (!recaptchaService.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "CAPTCHA isn't configured on this server yet");
        }
        if (!recaptchaService.verify(token)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CAPTCHA verification failed — please try again");
        }
    }

    private ResponseCookie sessionCookie(Long userId) {
        return ResponseCookie.from(JwtAuthenticationFilter.SESSION_COOKIE, jwtService.signSessionToken(userId))
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtService.sessionMaxAgeSeconds())
                .build();
    }
}
