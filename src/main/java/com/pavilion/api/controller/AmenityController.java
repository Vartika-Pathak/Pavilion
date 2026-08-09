package com.pavilion.api.controller;

import com.pavilion.api.amenities.AmenitiesCatalog;
import com.pavilion.api.amenities.AmenityDefinition;
import com.pavilion.api.amenities.AmenitySlots;
import com.pavilion.api.dto.AmenityDtos.AmenityResponse;
import com.pavilion.api.dto.AmenityDtos.AvailabilityResponse;
import com.pavilion.api.dto.AmenityDtos.BookAmenityRequest;
import com.pavilion.api.dto.AmenityDtos.BookAmenityResult;
import com.pavilion.api.dto.AmenityDtos.BookingResponse;
import com.pavilion.api.dto.AmenityDtos.ConfirmBookingRequest;
import com.pavilion.api.entity.AmenityBooking;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.AmenityBookingRepository;
import com.pavilion.api.service.StripeService;
import com.pavilion.api.service.StripeService.StripeSessionResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/amenities")
public class AmenityController {

    private final AmenityBookingRepository bookingRepository;
    private final StripeService stripeService;

    @Value("${app.public-url:http://localhost:5173}")
    private String defaultOrigin;

    public AmenityController(AmenityBookingRepository bookingRepository, StripeService stripeService) {
        this.bookingRepository = bookingRepository;
        this.stripeService = stripeService;
    }

    @GetMapping
    public List<AmenityResponse> listAmenities() {
        return AmenitiesCatalog.CATALOG.stream().map(AmenityResponse::from).toList();
    }

    @GetMapping("/availability")
    public AvailabilityResponse availability(@RequestParam String amenityId, @RequestParam String date) {
        AmenitiesCatalog.find(amenityId).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown amenity"));

        List<String> bookedSlots = bookingRepository.findByAmenityIdAndBookingDate(amenityId, date).stream()
                .map(AmenityBooking::getSlot)
                .toList();
        return new AvailabilityResponse(date, bookedSlots);
    }

    @GetMapping("/bookings/mine")
    public List<BookingResponse> myBookings(@AuthenticationPrincipal User user) {
        return bookingRepository.findByResidentId(user.getId()).stream()
                .map(booking -> BookingResponse.from(booking, user.getName(), user.getFlatNumber()))
                .toList();
    }

    @PostMapping("/bookings")
    public BookAmenityResult book(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody BookAmenityRequest body,
            @RequestHeader(value = "Origin", required = false) String originHeader) {
        AmenityDefinition amenity = AmenitiesCatalog.find(body.amenityId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown amenity"));

        if (AmenitySlots.isSlotPast(body.bookingDate(), body.slot(), Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That date and time slot has already passed");
        }

        if (bookingRepository.findByAmenityIdAndBookingDateAndSlot(body.amenityId(), body.bookingDate(), body.slot()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "That slot is already booked");
        }

        if (!amenity.requiresPayment()) {
            AmenityBooking booking = new AmenityBooking();
            booking.setResidentId(user.getId());
            booking.setAmenityId(amenity.id());
            booking.setBookingDate(body.bookingDate());
            booking.setSlot(body.slot());
            booking.setAmountPaidCents(0);
            booking = bookingRepository.save(booking);
            return new BookAmenityResult("confirmed", BookingResponse.from(booking, user.getName(), user.getFlatNumber()), null);
        }

        if (!stripeService.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Payments aren't configured on this server yet");
        }

        String origin = (originHeader != null && !originHeader.isBlank()) ? originHeader : defaultOrigin;
        var session = stripeService.createCheckoutSession(amenity, body.bookingDate(), body.slot(), user.getId(), origin);
        return new BookAmenityResult("requires_payment", null, session.url());
    }

    @PostMapping("/bookings/confirm")
    public BookingResponse confirm(@AuthenticationPrincipal User user, @Valid @RequestBody ConfirmBookingRequest body) {
        if (!stripeService.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Payments aren't configured on this server yet");
        }

        Optional<AmenityBooking> alreadyConfirmed = bookingRepository.findByStripeSessionId(body.sessionId());
        if (alreadyConfirmed.isPresent()) {
            return BookingResponse.from(alreadyConfirmed.get(), user.getName(), user.getFlatNumber());
        }

        StripeSessionResult session = stripeService.retrieveSession(body.sessionId());
        if (!"paid".equals(session.paymentStatus())) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Payment was not completed");
        }

        Map<String, String> metadata = session.metadata() == null ? Map.of() : session.metadata();
        String residentId = metadata.get("residentId");
        String amenityId = metadata.get("amenityId");
        String bookingDate = metadata.get("bookingDate");
        String slot = metadata.get("slot");
        if (residentId == null || amenityId == null || bookingDate == null || slot == null
                || !residentId.equals(String.valueOf(user.getId()))) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This payment session doesn't belong to you");
        }

        Optional<AmenityBooking> existing = bookingRepository.findByAmenityIdAndBookingDateAndSlot(amenityId, bookingDate, slot);
        if (existing.isPresent()) {
            if (session.paymentIntentId() != null) {
                stripeService.refund(session.paymentIntentId());
            }
            throw new ApiException(HttpStatus.CONFLICT,
                    "That slot was booked by someone else while you were paying — you've been refunded.");
        }

        if (AmenitySlots.isSlotPast(bookingDate, slot, Instant.now())) {
            if (session.paymentIntentId() != null) {
                stripeService.refund(session.paymentIntentId());
            }
            throw new ApiException(HttpStatus.CONFLICT, "That time slot passed while you were paying — you've been refunded.");
        }

        AmenityBooking booking = new AmenityBooking();
        booking.setResidentId(user.getId());
        booking.setAmenityId(amenityId);
        booking.setBookingDate(bookingDate);
        booking.setSlot(slot);
        booking.setAmountPaidCents(session.amountTotal() != null ? session.amountTotal().intValue() : 0);
        booking.setStripeSessionId(body.sessionId());
        booking = bookingRepository.save(booking);

        return BookingResponse.from(booking, user.getName(), user.getFlatNumber());
    }
}
