package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

// A maintenance payment recorded against a flat — flats aren't linked to resident accounts,
// so this is admin bookkeeping keyed by flat, with the payer's name typed in at entry time.
@Entity
@Table(name = "maintenance_collections")
public class MaintenanceCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "payer_name", nullable = false)
    private String payerName;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(name = "payment_date", nullable = false)
    private String paymentDate;

    // One of "cash", "cheque", "upi", "bank_transfer".
    @Column(name = "payment_mode", nullable = false)
    private String paymentMode;

    @Column(name = "for_month", nullable = false)
    private String forMonth;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getFlatId() {
        return flatId;
    }

    public void setFlatId(Long flatId) {
        this.flatId = flatId;
    }

    public String getPayerName() {
        return payerName;
    }

    public void setPayerName(String payerName) {
        this.payerName = payerName;
    }

    public Long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(Long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public String getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(String paymentDate) {
        this.paymentDate = paymentDate;
    }

    public String getPaymentMode() {
        return paymentMode;
    }

    public void setPaymentMode(String paymentMode) {
        this.paymentMode = paymentMode;
    }

    public String getForMonth() {
        return forMonth;
    }

    public void setForMonth(String forMonth) {
        this.forMonth = forMonth;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
