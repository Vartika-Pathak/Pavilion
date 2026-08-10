package com.pavilion.api.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "vendors")
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "contact_person_name", nullable = false)
    private String contactPersonName;

    @Column(name = "contact_number", nullable = false)
    private String contactNumber;

    @Column
    private String address;

    @Column(name = "gst_number")
    private String gstNumber;

    // One of "plumbing", "electrical", "appliance", "structural", "other", or null for a vendor
    // not tied to a maintenance category (e.g. a housekeeping or billing-only vendor). Used to
    // restrict which vendors can be assigned to a maintenance request of a given category.
    @Column
    private String category;

    // Smallest currency unit (paise), matching the integer-money convention used elsewhere.
    @Column(name = "opening_balance_paise", nullable = false)
    private Long openingBalancePaise = 0L;

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getContactPersonName() {
        return contactPersonName;
    }

    public void setContactPersonName(String contactPersonName) {
        this.contactPersonName = contactPersonName;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGstNumber() {
        return gstNumber;
    }

    public void setGstNumber(String gstNumber) {
        this.gstNumber = gstNumber;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getOpeningBalancePaise() {
        return openingBalancePaise;
    }

    public void setOpeningBalancePaise(Long openingBalancePaise) {
        this.openingBalancePaise = openingBalancePaise;
    }
}
