package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

// A one-off charge announced to every flat (e.g. a festival fund, a repair levy) — distinct
// from the recurring per-flat-type maintenance rate.
@Entity
@Table(name = "special_contributions")
public class SpecialContribution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "amount_paise", nullable = false)
    private Long amountPaise;

    @Column(name = "due_date", nullable = false)
    private String dueDate;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getAmountPaise() {
        return amountPaise;
    }

    public void setAmountPaise(Long amountPaise) {
        this.amountPaise = amountPaise;
    }

    public String getDueDate() {
        return dueDate;
    }

    public void setDueDate(String dueDate) {
        this.dueDate = dueDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
