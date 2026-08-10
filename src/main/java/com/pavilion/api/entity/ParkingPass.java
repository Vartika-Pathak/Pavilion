package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

// One row per flat: parking is purchased once and grants that flat vehicle-registration
// access forever, unlike AmenityBooking which is a repeated per-slot booking.
@Entity
@Table(name = "parking_passes")
public class ParkingPass {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flat_number", nullable = false, unique = true)
    private String flatNumber;

    @Column(name = "purchased_by_resident_id", nullable = false)
    private Long purchasedByResidentId;

    @Column(name = "purchased_by_name", nullable = false)
    private String purchasedByName;

    @Column(name = "amount_paid_cents", nullable = false)
    private Integer amountPaidCents;

    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getFlatNumber() {
        return flatNumber;
    }

    public void setFlatNumber(String flatNumber) {
        this.flatNumber = flatNumber;
    }

    public Long getPurchasedByResidentId() {
        return purchasedByResidentId;
    }

    public void setPurchasedByResidentId(Long purchasedByResidentId) {
        this.purchasedByResidentId = purchasedByResidentId;
    }

    public String getPurchasedByName() {
        return purchasedByName;
    }

    public void setPurchasedByName(String purchasedByName) {
        this.purchasedByName = purchasedByName;
    }

    public Integer getAmountPaidCents() {
        return amountPaidCents;
    }

    public void setAmountPaidCents(Integer amountPaidCents) {
        this.amountPaidCents = amountPaidCents;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public void setStripeSessionId(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
