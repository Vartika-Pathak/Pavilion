package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "maintenance_requests")
public class MaintenanceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "resident_name", nullable = false)
    private String residentName;

    @Column(name = "resident_flat_number", nullable = false)
    private String residentFlatNumber;

    // One of "plumbing", "electrical", "appliance", "structural", "other".
    @Column(nullable = false)
    private String category;

    @Column(nullable = false)
    private String description;

    @Convert(converter = StringListJsonConverter.class)
    @Column(name = "photo_urls", nullable = false)
    private List<String> photoUrls = List.of();

    // One of "open", "in_progress", "resolved", "closed".
    @Column(nullable = false)
    private String status = "open";

    @Column(name = "vendor_id")
    private Long vendorId;

    // Denormalized so the assignment stays visible even if the vendor is later renamed or deleted
    // — same pattern as residentName/residentFlatNumber above.
    @Column(name = "vendor_name")
    private String vendorName;

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

    public List<String> getPhotoUrls() {
        return photoUrls;
    }

    public void setPhotoUrls(List<String> photoUrls) {
        this.photoUrls = photoUrls;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getVendorId() {
        return vendorId;
    }

    public void setVendorId(Long vendorId) {
        this.vendorId = vendorId;
    }

    public String getVendorName() {
        return vendorName;
    }

    public void setVendorName(String vendorName) {
        this.vendorName = vendorName;
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
