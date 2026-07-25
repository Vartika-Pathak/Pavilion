package com.pavilion.api.dto;

import com.pavilion.api.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
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
