package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "bill_payments")
public class BillPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vendor_bill_id", nullable = false)
    private Long vendorBillId;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(name = "payment_date", nullable = false)
    private String paymentDate;

    // One of "cash", "cheque", "upi", "bank_transfer".
    @Column(name = "payment_mode", nullable = false)
    private String paymentMode;

    @Column(name = "reference_number")
    private String referenceNumber;

    @Column
    private String notes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public Long getVendorBillId() {
        return vendorBillId;
    }

    public void setVendorBillId(Long vendorBillId) {
        this.vendorBillId = vendorBillId;
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
