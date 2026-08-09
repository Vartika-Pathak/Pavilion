package com.pavilion.api.entity;

import jakarta.persistence.*;

// One row per flat type — the monthly maintenance amount charged to flats of that type.
@Entity
@Table(name = "maintenance_rates")
public class MaintenanceRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "flat_type", nullable = false, unique = true)
    private String flatType;

    @Column(name = "monthly_amount_paise", nullable = false)
    private Long monthlyAmountPaise = 0L;

    public Long getId() {
        return id;
    }

    public String getFlatType() {
        return flatType;
    }

    public void setFlatType(String flatType) {
        this.flatType = flatType;
    }

    public Long getMonthlyAmountPaise() {
        return monthlyAmountPaise;
    }

    public void setMonthlyAmountPaise(Long monthlyAmountPaise) {
        this.monthlyAmountPaise = monthlyAmountPaise;
    }
}
