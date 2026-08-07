package com.pavilion.api.dto;

import com.pavilion.api.entity.PendingSignup;
import com.pavilion.api.entity.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;

public class AuthDtos {

    public record SignupRequest(
            @NotBlank(message = "Name is required")
            @Pattern(regexp = "^[A-Za-z ]{2,100}$", message = "Name can only contain letters and spaces")
            String name,
            @NotBlank @Email(message = "Enter a valid email address") String email,
            @NotBlank(message = "Flat number is required")
            @Pattern(regexp = "^[A-Za-z]-[0-9]{1,3}$", message = "Flat number must be a letter, a hyphen, then 1-3 digits, e.g. A-100")
            String flatNumber,
            @NotBlank(message = "Password is required")
            // Capped at 72: BCrypt (the hash this ends up in) silently ignores anything past 72
            // bytes, so a longer password would just be truncated rather than actually used.
            @Size(min = 8, max = 72, message = "Password must be between 8 and 72 characters")
            String password,
            @NotBlank String captchaToken,
            // Optional — populated when the resident indicated on the signup form that they have
            // family living with them. Null/omitted means no family members to record.
            List<@Valid FamilyMemberInput> familyMembers) {
    }

    public record FamilyMemberInput(
            @NotBlank(message = "Family member name is required")
            @Pattern(regexp = "^[A-Za-z ]{2,100}$", message = "Family member name can only contain letters and spaces")
            String name,
            @NotBlank(message = "Relation is required")
            @Pattern(regexp = "^[A-Za-z ]{2,50}$", message = "Relation can only contain letters and spaces")
            String relation,
            @Min(value = 0, message = "Age must be a positive number")
            @Max(value = 120, message = "Age must be realistic")
            Integer age) {
    }

    public record SubmitVerificationRequest(
            @NotBlank(message = "Name is required")
            @Pattern(regexp = "^[A-Za-z ]{2,100}$", message = "Name can only contain letters and spaces")
            String name,
            @NotBlank(message = "Flat number is required")
            @Pattern(regexp = "^[A-Za-z]-[0-9]{1,3}$", message = "Flat number must be a letter, a hyphen, then 1-3 digits, e.g. A-100")
            String flatNumber) {
    }

    /** status is one of "pending", "approved", "rejected", or "not_found" (no matching request yet). */
    public record VerificationStatusResponse(String status, String message) {
    }

    public record LoginRequest(
            @NotBlank @Email(message = "Enter a valid email address") String email,
            @NotBlank String password,
            @NotBlank String captchaToken) {
    }

    public record VerifySignupOtpRequest(
            @NotNull Long pendingSignupId,
            @NotBlank
            @Pattern(regexp = "^\\d{6}$", message = "Enter the 6-digit code")
            String otpCode) {
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
