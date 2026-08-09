package com.pavilion.api.entity;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "amenity_bookings")
public class AmenityBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "amenity_id", nullable = false)
    private String amenityId;

    @Column(name = "booking_date", nullable = false)
    private String bookingDate;

    @Column(nullable = false)
    private String slot;

    @Column(name = "amount_paid_cents", nullable = false)
    private Integer amountPaidCents = 0;

    @Column(name = "stripe_session_id")
    private String stripeSessionId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getResidentId() {
        return residentId;
    }

    public void setResidentId(Long residentId) {
        this.residentId = residentId;
    }

    public String getAmenityId() {
        return amenityId;
    }

    public void setAmenityId(String amenityId) {
        this.amenityId = amenityId;
    }

    public String getBookingDate() {
        return bookingDate;
    }

    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
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
