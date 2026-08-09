package com.pavilion.api.dto;

import com.pavilion.api.amenities.AmenitiesCatalog;
import com.pavilion.api.amenities.AmenityDefinition;
import com.pavilion.api.entity.AmenityBooking;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;

public class AmenityDtos {

    public record AmenityResponse(String id, String name, String description, boolean requiresPayment, int priceCents) {
        public static AmenityResponse from(AmenityDefinition amenity) {
            return new AmenityResponse(
                    amenity.id(), amenity.name(), amenity.description(), amenity.requiresPayment(), amenity.priceCents());
        }
    }

    public record AvailabilityResponse(String date, List<String> bookedSlots) {
    }

    public record BookingResponse(
            Long id, String amenityId, String amenityName, String bookingDate, String slot,
            Integer amountPaidCents, String residentName, String residentFlatNumber, Instant createdAt) {
        public static BookingResponse from(AmenityBooking booking, String residentName, String residentFlatNumber) {
            String amenityName = AmenitiesCatalog.find(booking.getAmenityId())
                    .map(AmenityDefinition::name)
                    .orElse(booking.getAmenityId());
            return new BookingResponse(
                    booking.getId(), booking.getAmenityId(), amenityName, booking.getBookingDate(), booking.getSlot(),
                    booking.getAmountPaidCents(), residentName, residentFlatNumber, booking.getCreatedAt());
        }
    }

    public record BookAmenityRequest(
            @NotBlank(message = "Amenity is required") String amenityId,
            @NotBlank(message = "Booking date is required")
            @Pattern(regexp = "^\\d{4}-\\d{2}-\\d{2}$", message = "bookingDate must be in YYYY-MM-DD format")
            String bookingDate,
            @NotBlank(message = "Slot is required")
            @Pattern(regexp = "^(morning|afternoon|evening)$", message = "slot must be morning, afternoon, or evening")
            String slot) {
    }

    public record BookAmenityResult(String status, BookingResponse booking, String checkoutUrl) {
    }

    public record ConfirmBookingRequest(@NotBlank(message = "Session id is required") String sessionId) {
    }
}
