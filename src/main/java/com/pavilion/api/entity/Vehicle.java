package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "vehicles")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resident_id", nullable = false)
    private Long residentId;

    @Column(name = "plate_number", nullable = false)
    private String plateNumber;

    // One of "car", "bike", "other".
    @Column(name = "vehicle_type", nullable = false)
    private String vehicleType;

    // Denormalized from the resident's own account at creation time — same pattern as
    // MaintenanceRequest.residentName/residentFlatNumber.
    @Column(name = "owner_name", nullable = false)
    private String ownerName;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Column(name = "owner_phone", nullable = false)
    private String ownerPhone;

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

    public String getPlateNumber() {
        return plateNumber;
    }

    public void setPlateNumber(String plateNumber) {
        this.plateNumber = plateNumber;
    }

    public String getVehicleType() {
        return vehicleType;
    }

    public void setVehicleType(String vehicleType) {
        this.vehicleType = vehicleType;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }

    public String getFlatNumber() {
        return flatNumber;
    }

    public void setFlatNumber(String flatNumber) {
        this.flatNumber = flatNumber;
    }

    public String getOwnerPhone() {
        return ownerPhone;
    }

    public void setOwnerPhone(String ownerPhone) {
        this.ownerPhone = ownerPhone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
