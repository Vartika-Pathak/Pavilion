package com.pavilion.api.dto;

import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Visit;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public class VisitDtos {

    public record CreateVisitRequest(
            @NotBlank @Pattern(regexp = "cab_delivery|guest|household_help|maintenance_staff") String visitType,
            @NotBlank(message = "Visitor name is required")
            @Pattern(regexp = "^[A-Za-z ]{2,100}$", message = "Visitor name can only contain letters and spaces")
            String visitorName,
            // Optional (a visit doesn't require a phone number), but if one is given it has to be
            // a real 10-digit mobile number — @Pattern only runs against non-null values, and the
            // "^$|..." alternative lets an empty string through too, so this can't reject someone
            // simply not filling the field in.
            @Pattern(regexp = ValidationPatterns.OPTIONAL_PHONE_10_DIGIT,
                    message = "Mobile number must be exactly 10 digits, starting with 6-9")
            String visitorPhone,
            @Email(message = "Enter a valid email address") String visitorEmail) {

        // Normalizes natural typing ("98765 43210") before the @Pattern check above runs. Leaves
        // null alone (no phone provided) rather than turning it into an empty string.
        public CreateVisitRequest {
            if (visitorPhone != null) {
                visitorPhone = visitorPhone.trim().replaceAll("[\\s-]", "");
            }
        }
    }

    public record LookupVisitRequest(
            @NotBlank
            @Pattern(regexp = "^\\d{6}$", message = "Enter the 6-digit code")
            String otpCode) {
    }

    public record DecideVisitRequest(boolean approve) {
    }

    public record ConfirmVisitRequest(
            @NotBlank
            @Pattern(regexp = "^\\d{6}$", message = "Enter the 6-digit code")
            String otpCode) {
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

    // Every visit ever logged, for the society-wide entry log (guard/admin) — unlike
    // VisitLookupResult (gate flow, pending only), this covers every status.
    public record VisitLogEntry(
            Long id,
            String visitType,
            String visitorName,
            String visitorPhone,
            String status,
            Instant expiresAt,
            Instant createdAt,
            String residentName,
            String residentFlatNumber) {

        public static VisitLogEntry from(Visit visit, User resident) {
            return new VisitLogEntry(
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
