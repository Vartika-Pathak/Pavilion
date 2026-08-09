package com.pavilion.api.entity;

import jakarta.persistence.*;

// There's only ever one row — callers fetch-or-create it rather than picking a row to update.
@Entity
@Table(name = "maintenance_settings")
public class MaintenanceSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "due_day", nullable = false)
    private Integer dueDay = 10;

    @Column(name = "late_fee_percent", nullable = false)
    private Integer lateFeePercent = 0;

    @Column(name = "opening_balance_note", nullable = false)
    private String openingBalanceNote = "";

    public Long getId() {
        return id;
    }

    public Integer getDueDay() {
        return dueDay;
    }

    public void setDueDay(Integer dueDay) {
        this.dueDay = dueDay;
    }

    public Integer getLateFeePercent() {
        return lateFeePercent;
    }

    public void setLateFeePercent(Integer lateFeePercent) {
        this.lateFeePercent = lateFeePercent;
    }

    public String getOpeningBalanceNote() {
        return openingBalanceNote;
    }

    public void setOpeningBalanceNote(String openingBalanceNote) {
        this.openingBalanceNote = openingBalanceNote;
    }
}
