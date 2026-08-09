package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

// resident_id is not a foreign key to User on purpose: it's a plain reference kept for
// ownership filtering, denormalized alongside residentName/residentFlatNumber the same way
// the rest of this table works, rather than joined at read time.
@Entity
@Table(name = "complaints")
public class Complaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "resident_name", nullable = false)
    private String residentName;

    @Column(name = "resident_flat_number", nullable = false)
    private String residentFlatNumber;

    // One of "maintenance", "security", "noise", "other".
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    // One of "open", "in_progress", "resolved".
    @Column(nullable = false)
    private String status = "open";

    @Column(name = "resolution_note")
    private String resolutionNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getId() {
        return id;
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

    public String getResidentFlatNumber() {
        return residentFlatNumber;
    }

    public void setResidentFlatNumber(String residentFlatNumber) {
        this.residentFlatNumber = residentFlatNumber;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
