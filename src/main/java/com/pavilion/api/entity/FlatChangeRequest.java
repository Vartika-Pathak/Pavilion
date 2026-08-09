package com.pavilion.api.entity;

import jakarta.persistence.*;

import java.time.Instant;

// A resident asking the admin to correct their flat's details — not a direct edit, since Flat
// Resident data stays admin-managed; this is just a notification with context attached.
@Entity
@Table(name = "flat_change_requests")
public class FlatChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flat_id", nullable = false)
    private Long flatId;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "resident_name", nullable = false)
    private String residentName;

    @Column(nullable = false)
    private String message;

    // "pending" or "reviewed".
    @Column(nullable = false)
    private String status = "pending";

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

    public Long getResidentId() {
        return residentId;
    }

    public void setResidentId(Long residentId) {
        this.residentId = residentId;
    }

    public String getResidentName() {
        return residentName;
    }

    public void setResidentName(String residentName) {
        this.residentName = residentName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
