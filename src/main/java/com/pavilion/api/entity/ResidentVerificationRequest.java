package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * A first-time resident's request to be let into the signup flow — submitted with just a flat
 * number and name, then reviewed by an admin (documents + payment confirmed some other way, not
 * through this app) before being approved or rejected. Approval is what {@code /verify-resident}
 * used to do automatically against a static allowlist; now a human decides.
 */
@Entity
@Table(name = "resident_verification_requests")
public class ResidentVerificationRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Column(nullable = false)
    private String name;

    @Column(name = "documents_verified", nullable = false)
    private boolean documentsVerified = false;

    @Column(name = "payment_received", nullable = false)
    private boolean paymentReceived = false;

    /** "pending", "approved", or "rejected". */
    @Column(nullable = false)
    private String status = "pending";

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDocumentsVerified() {
        return documentsVerified;
    }

    public void setDocumentsVerified(boolean documentsVerified) {
        this.documentsVerified = documentsVerified;
    }

    public boolean isPaymentReceived() {
        return paymentReceived;
    }

    public void setPaymentReceived(boolean paymentReceived) {
        this.paymentReceived = paymentReceived;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getReviewedBy() {
        return reviewedBy;
    }

    public void setReviewedBy(Long reviewedBy) {
        this.reviewedBy = reviewedBy;
    }

    public Instant getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(Instant reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
