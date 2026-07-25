package com.pavilion.api.controller;

import com.pavilion.api.dto.AuthDtos.AuthUserResponse;
import com.pavilion.api.dto.AuthDtos.LoginRequest;
import com.pavilion.api.dto.AuthDtos.SignupRequest;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.UserRepository;
import com.pavilion.api.security.CurrentUserResolver;
import com.pavilion.api.security.JwtService;
import com.pavilion.api.security.RecaptchaService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final CurrentUserResolver currentUserResolver;
    private final RecaptchaService recaptchaService;

    public AuthController(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            CurrentUserResolver currentUserResolver,
            RecaptchaService recaptchaService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.currentUserResolver = currentUserResolver;
        this.recaptchaService = recaptchaService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthUserResponse> signup(@Valid @RequestBody SignupRequest body) {
        requireValidCaptcha(body.captchaToken());

        if (userRepository.findByEmail(body.email()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "An account with this email already exists");
        }

        User user = new User();
        user.setName(body.name());
        user.setEmail(body.email());
        user.setFlatNumber(body.flatNumber());
        user.setPasswordHash(passwordEncoder.encode(body.password()));
        user = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, sessionCookie(user.getId()).toString())
                .body(AuthUserResponse.from(user));
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
