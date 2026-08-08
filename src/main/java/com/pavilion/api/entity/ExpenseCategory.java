package com.pavilion.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "expense_categories")
public class ExpenseCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "gst_slab_percent", nullable = false)
    private Integer gstSlabPercent;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getGstSlabPercent() {
        return gstSlabPercent;
    }

    public void setGstSlabPercent(Integer gstSlabPercent) {
        this.gstSlabPercent = gstSlabPercent;
    }
}
