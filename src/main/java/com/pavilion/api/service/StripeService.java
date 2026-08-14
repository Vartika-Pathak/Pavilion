package com.pavilion.api.service;

import com.pavilion.api.amenities.AmenityDefinition;
import com.pavilion.api.exception.ApiException;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.model.checkout.Session;
import com.stripe.param.RefundCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;

// Wraps the Stripe SDK behind a small, mockable surface so AmenityController doesn't talk to
// Stripe directly — lets tests exercise the full paid-booking flow (checkout -> confirm ->
// race/refund handling) without hitting the real Stripe API.
@Service
public class StripeService {

    private static final Logger log = LoggerFactory.getLogger(StripeService.class);

    @Value("${stripe.secret-key:}")
    private String secretKey;

    @PostConstruct
    void init() {
        if (isConfigured()) {
            Stripe.apiKey = secretKey;
        } else {
            log.warn("STRIPE_SECRET_KEY is not set — booking a paid amenity will fail until it's "
                    + "configured. Free amenities are unaffected.");
        }
    }

    public boolean isConfigured() {
        return secretKey != null && !secretKey.isBlank();
    }

    public CheckoutSessionResult createCheckoutSession(
            AmenityDefinition amenity, String bookingDate, String slot, Long residentId, String origin) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("inr")
                                    .setUnitAmount((long) amenity.priceCents())
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName(amenity.name() + " — " + bookingDate + " (" + slot + ")")
                                            .build())
                                    .build())
                            .build())
                    .putMetadata("residentId", String.valueOf(residentId))
                    .putMetadata("amenityId", amenity.id())
                    .putMetadata("bookingDate", bookingDate)
                    .putMetadata("slot", slot)
                    .setSuccessUrl(origin + "/amenities?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(origin + "/amenities")
                    .build();
            Session session = Session.create(params);
            return new CheckoutSessionResult(session.getId(), session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe checkout session creation failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Couldn't start payment — please try again");
        }
    }

    // Stripe's unit_amount for INR is already the smallest unit (paise), same as our own storage,
    // so no conversion is needed.
    public CheckoutSessionResult createMaintenanceCheckoutSession(
            long amountPaise, String buildingName, String flatNumber, Long flatId, String forMonth, String origin) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("inr")
                                    .setUnitAmount(amountPaise)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Maintenance — " + flatNumber + ", " + buildingName + " (" + forMonth + ")")
                                            .build())
                                    .build())
                            .build())
                    .putMetadata("flatId", String.valueOf(flatId))
                    .putMetadata("forMonth", forMonth)
                    .setSuccessUrl(origin + "/pay-maintenance?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(origin + "/pay-maintenance")
                    .build();
            Session session = Session.create(params);
            return new CheckoutSessionResult(session.getId(), session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe maintenance checkout session creation failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Couldn't start payment — please try again");
        }
    }

    // Like the maintenance checkout, this is a one-off INR charge — no bookingDate/slot metadata
    // since a parking pass isn't tied to a date, just to the flat that bought it.
    public CheckoutSessionResult createParkingPassCheckoutSession(
            long amountPaise, String flatNumber, Long residentId, String origin) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setQuantity(1L)
                            .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                    .setCurrency("inr")
                                    .setUnitAmount(amountPaise)
                                    .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                            .setName("Parking Pass — Flat " + flatNumber)
                                            .build())
                                    .build())
                            .build())
                    .putMetadata("residentId", String.valueOf(residentId))
                    .putMetadata("flatNumber", flatNumber)
                    .setSuccessUrl(origin + "/amenities?parking_session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(origin + "/amenities")
                    .build();
            Session session = Session.create(params);
            return new CheckoutSessionResult(session.getId(), session.getUrl());
        } catch (StripeException e) {
            log.error("Stripe parking pass checkout session creation failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Couldn't start payment — please try again");
        }
    }

    public StripeSessionResult retrieveSession(String sessionId) {
        try {
            Session session = Session.retrieve(sessionId);
            return new StripeSessionResult(
                    session.getPaymentStatus(), session.getPaymentIntent(), session.getAmountTotal(), session.getMetadata());
        } catch (StripeException e) {
            log.error("Stripe session retrieval failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Couldn't verify payment — please try again");
        }
    }

    public void refund(String paymentIntentId) {
        try {
            RefundCreateParams params = RefundCreateParams.builder().setPaymentIntent(paymentIntentId).build();
            Refund.create(params);
        } catch (StripeException e) {
            log.error("Stripe refund failed for payment intent {}", paymentIntentId, e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "Couldn't process a required refund — please contact support");
        }
    }

    public record CheckoutSessionResult(String id, String url) {
    }

    public record StripeSessionResult(String paymentStatus, String paymentIntentId, Long amountTotal, Map<String, String> metadata) {
    }
}
