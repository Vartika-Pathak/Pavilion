package com.pavilion.api.controller;

import com.pavilion.api.dto.AuthDtos.AuthUserResponse;
import com.pavilion.api.dto.AuthDtos.LoginRequest;
import com.pavilion.api.dto.AuthDtos.SignupPendingResponse;
import com.pavilion.api.dto.AuthDtos.SignupRequest;
import com.pavilion.api.dto.AuthDtos.VerifySignupOtpRequest;
import com.pavilion.api.entity.PendingSignup;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.PendingSignupRepository;
import com.pavilion.api.repository.UserRepository;
import com.pavilion.api.security.CurrentUserResolver;
import com.pavilion.api.security.JwtService;
import com.pavilion.api.security.RecaptchaService;
import com.pavilion.api.service.EmailService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PendingSignupRepository pendingSignupRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserResolver currentUserResolver;
    private final RecaptchaService recaptchaService;
    private final EmailService emailService;

    public AuthController(
            UserRepository userRepository,
            PendingSignupRepository pendingSignupRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CurrentUserResolver currentUserResolver,
            RecaptchaService recaptchaService,
            EmailService emailService) {
        this.userRepository = userRepository;
        this.pendingSignupRepository = pendingSignupRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUserResolver = currentUserResolver;
        this.recaptchaService = recaptchaService;
        this.emailService = emailService;
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

        pendingSignupRepository.delete(pending);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, sessionCookie(user.getId()).toString())
                .body(AuthUserResponse.from(user));
    }

    private static String generateOtpCode() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
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
        ResponseCookie cleared = ResponseCookie.from(CurrentUserResolver.SESSION_COOKIE, "")
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(0)
                .build();
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cleared.toString()).build();
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(HttpServletRequest request) {
        User user = currentUserResolver.resolve(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not signed in"));
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
        return ResponseCookie.from(CurrentUserResolver.SESSION_COOKIE, jwtService.signSessionToken(userId))
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(jwtService.sessionMaxAgeSeconds())
                .build();
    }
}
