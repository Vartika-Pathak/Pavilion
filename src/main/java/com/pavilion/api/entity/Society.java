package com.pavilion.api.entity;

import jakarta.persistence.*;

// There's only ever one row — callers fetch-or-create it rather than picking a row to update.
@Entity
@Table(name = "society")
public class Society {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name = "";

    @Column(nullable = false)
    private String address = "";

    @Column(name = "contact_number", nullable = false)
    private String contactNumber = "";

    @Column(nullable = false)
    private String email = "";

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getContactNumber() {
        return contactNumber;
    }

    public void setContactNumber(String contactNumber) {
        this.contactNumber = contactNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
