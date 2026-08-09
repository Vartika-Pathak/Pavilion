package com.pavilion.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "maintenance_discounts")
public class MaintenanceDiscount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    // "percent" or "fixed".
    @Column(name = "discount_type", nullable = false)
    private String discountType;

    // Percent: 0-100. Fixed: paise.
    @Column(nullable = false)
    private Long value;

    @Column
    private String description;

    @Column(nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDiscountType() {
        return discountType;
    }

    public void setDiscountType(String discountType) {
        this.discountType = discountType;
    }

    public Long getValue() {
        return value;
    }

    public void setValue(Long value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
