package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

/**
 * Committee-maintained allowlist of who's permitted to sign up for a given flat — checked by
 * {@code POST /api/auth/verify-resident} before a first-time resident is allowed into the signup
 * form. There's no admin UI for this yet, so rows are added directly (a migration or a DB console
 * insert) by whoever manages the deployment.
 */
@Entity
@Table(name = "approved_residents")
public class ApprovedResident {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Column(nullable = false)
    private String name;

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

    public Instant getCreatedAt() {
        return createdAt;
    }
}
