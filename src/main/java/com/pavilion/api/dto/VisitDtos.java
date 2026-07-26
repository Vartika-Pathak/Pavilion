package com.pavilion.api.dto;

import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Visit;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public class VisitDtos {

    public record CreateVisitRequest(
            @NotBlank @Pattern(regexp = "cab_delivery|guest|household_help") String visitType,
            @NotBlank String visitorName,
            String visitorPhone,
            @Email String visitorEmail) {
    }

    public record LookupVisitRequest(@NotBlank String otpCode) {
    }

    public record DecideVisitRequest(boolean approve) {
    }

    public record ConfirmVisitRequest(@NotBlank String otpCode) {
    }

    public record VisitResponse(
            Long id,
            String visitType,
            String visitorName,
            String visitorPhone,
            String visitorEmail,
            String otpCode,
            String status,
            Instant expiresAt,
            Instant createdAt) {

        public static VisitResponse from(Visit visit) {
            // While awaiting email verification, the resident hasn't proven they can reach the
            // visitor's inbox yet — don't hand the code back until they confirm it.
            boolean hideOtp = "awaiting_verification".equals(visit.getStatus());
            return new VisitResponse(
                    visit.getId(),
                    visit.getVisitType(),
                    visit.getVisitorName(),
                    visit.getVisitorPhone(),
                    visit.getVisitorEmail(),
                    hideOtp ? null : visit.getOtpCode(),
                    visit.getStatus(),
                    visit.getExpiresAt(),
                    visit.getCreatedAt());
        }
    }

    public record VisitLookupResult(
            Long id,
            String visitType,
            String visitorName,
            String visitorPhone,
            String status,
            Instant expiresAt,
            Instant createdAt,
            String residentName,
            String residentFlatNumber) {

        public static VisitLookupResult from(Visit visit, User resident) {
            return new VisitLookupResult(
                    visit.getId(),
                    visit.getVisitType(),
                    visit.getVisitorName(),
                    visit.getVisitorPhone(),
                    visit.getStatus(),
                    visit.getExpiresAt(),
                    visit.getCreatedAt(),
                    resident.getName(),
                    resident.getFlatNumber());
        }
    }
}
