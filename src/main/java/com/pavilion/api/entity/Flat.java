package com.pavilion.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "flats")
public class Flat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "building_id", nullable = false)
    private Long buildingId;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    // One of "1bhk", "2bhk", "3bhk", "4bhk" — validated at the DTO layer rather than
    // modeled as a JPA enum, matching how the rest of Masters keeps roles/types as plain strings.
    @Column(name = "flat_type", nullable = false)
    private String flatType;

    @Column(nullable = false)
    private boolean occupied = false;

    // "owner" or "rented".
    @Column(name = "ownership_type", nullable = false)
    private String ownershipType = "owner";

    // Null until an admin assigns a resident account to this flat — a flat can exist (e.g. newly
    // built, vacant) before anyone lives in it.
    @Column(name = "resident_id")
    private Long residentId;

    public Long getId() {
        return id;
    }

    public Long getBuildingId() {
        return buildingId;
    }

    public void setBuildingId(Long buildingId) {
        this.buildingId = buildingId;
    }

    public String getFlatNumber() {
        return flatNumber;
    }

    public void setFlatNumber(String flatNumber) {
        this.flatNumber = flatNumber;
    }

    public String getFlatType() {
        return flatType;
    }

    public void setFlatType(String flatType) {
        this.flatType = flatType;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void setOccupied(boolean occupied) {
        this.occupied = occupied;
    }

    public String getOwnershipType() {
        return ownershipType;
    }

    public void setOwnershipType(String ownershipType) {
        this.ownershipType = ownershipType;
    }

    public Long getResidentId() {
        return residentId;
    }

    public void setResidentId(Long residentId) {
        this.residentId = residentId;
    }
}
