package com.pavilion.api.controller;

import com.pavilion.api.dto.ParkingPassDtos.ConfirmParkingPassRequest;
import com.pavilion.api.dto.ParkingPassDtos.ParkingPassResponse;
import com.pavilion.api.dto.ParkingPassDtos.PurchaseParkingPassResult;
import com.pavilion.api.entity.ParkingPass;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.ParkingPassRepository;
import com.pavilion.api.service.StripeService;
import com.pavilion.api.service.StripeService.StripeSessionResult;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

// A parking pass is bought once per flat and never expires — unlike AmenityBooking, there's no
// date/slot to it, just a flat-level flag that Vehicle registration checks before letting a
// resident add a vehicle.
@RestController
@RequestMapping("/api/parking")
public class ParkingPassController {

    // ₹5,000, one-time, per flat.
    private static final long PARKING_PASS_PRICE_PAISE = 500_000L;

    private final ParkingPassRepository parkingPassRepository;
    private final StripeService stripeService;

    @Value("${app.public-url:http://localhost:5173}")
    private String defaultOrigin;

    public ParkingPassController(ParkingPassRepository parkingPassRepository, StripeService stripeService) {
        this.parkingPassRepository = parkingPassRepository;
        this.stripeService = stripeService;
    }

    @GetMapping
    public ParkingPassResponse myPass(@AuthenticationPrincipal User user) {
        return parkingPassRepository.findByFlatNumber(user.getFlatNumber())
                .map(ParkingPassResponse::from)
                .orElse(null);
    }

    @PostMapping("/purchase")
    public PurchaseParkingPassResult purchase(
            @AuthenticationPrincipal User user, @RequestHeader(value = "Origin", required = false) String originHeader) {
        if (parkingPassRepository.findByFlatNumber(user.getFlatNumber()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT, "Your flat already has a parking pass");
        }

        if (!stripeService.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Payments aren't configured on this server yet");
        }

        String origin = (originHeader != null && !originHeader.isBlank()) ? originHeader : defaultOrigin;
        var session = stripeService.createParkingPassCheckoutSession(
                PARKING_PASS_PRICE_PAISE, user.getFlatNumber(), user.getId(), origin);
        return new PurchaseParkingPassResult("requires_payment", null, session.url());
    }

    @PostMapping("/confirm")
    public ParkingPassResponse confirm(@AuthenticationPrincipal User user, @Valid @RequestBody ConfirmParkingPassRequest body) {
        if (!stripeService.isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "Payments aren't configured on this server yet");
        }

        Optional<ParkingPass> alreadyConfirmed = parkingPassRepository.findByStripeSessionId(body.sessionId());
        if (alreadyConfirmed.isPresent()) {
            return ParkingPassResponse.from(alreadyConfirmed.get());
        }

        StripeSessionResult session = stripeService.retrieveSession(body.sessionId());
        if (!"paid".equals(session.paymentStatus())) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Payment was not completed");
        }

        Map<String, String> metadata = session.metadata() == null ? Map.of() : session.metadata();
        String residentId = metadata.get("residentId");
        String flatNumber = metadata.get("flatNumber");
        if (residentId == null || flatNumber == null
                || !residentId.equals(String.valueOf(user.getId()))
                || !flatNumber.equals(user.getFlatNumber())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This payment session doesn't belong to you");
        }

        Optional<ParkingPass> existing = parkingPassRepository.findByFlatNumber(flatNumber);
        if (existing.isPresent()) {
            if (session.paymentIntentId() != null) {
                stripeService.refund(session.paymentIntentId());
            }
            throw new ApiException(HttpStatus.CONFLICT,
                    "Your flat already has a parking pass — you've been refunded.");
        }

        ParkingPass pass = new ParkingPass();
        pass.setFlatNumber(flatNumber);
        pass.setPurchasedByResidentId(user.getId());
        pass.setPurchasedByName(user.getName());
        pass.setAmountPaidCents(session.amountTotal() != null ? session.amountTotal().intValue() : 0);
        pass.setStripeSessionId(body.sessionId());
        pass = parkingPassRepository.save(pass);

        return ParkingPassResponse.from(pass);
    }
}
