package com.pavilion.api.dto;

import com.pavilion.api.entity.PendingSignup;
import com.pavilion.api.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class AuthDtos {

    public record SignupRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank String flatNumber,
            @NotBlank @Size(min = 8) String password,
            @NotBlank String captchaToken) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            @NotBlank String captchaToken) {
    }

    public record VerifySignupOtpRequest(
            @NotNull Long pendingSignupId,
            @NotBlank String otpCode) {
    }

    /** Returned after /signup — the account isn't created yet until the emailed OTP is verified. */
    public record SignupPendingResponse(Long pendingSignupId, String email) {

        public static SignupPendingResponse from(PendingSignup pending) {
            return new SignupPendingResponse(pending.getId(), pending.getEmail());
        }
    }

    public record AuthUserResponse(
            Long id,
            String name,
            String email,
            String flatNumber,
            String role,
            Instant createdAt) {

        public static AuthUserResponse from(User user) {
            return new AuthUserResponse(
                    user.getId(),
                    user.getName(),
                    user.getEmail(),
                    user.getFlatNumber(),
                    user.getRole(),
                    user.getCreatedAt());
        }
    }
}
