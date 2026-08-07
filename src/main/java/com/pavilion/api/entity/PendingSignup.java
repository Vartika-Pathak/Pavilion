package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "pending_signups")
public class PendingSignup {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Column(name = "otp_code", nullable = false)
    private String otpCode;

    /** JSON-serialized list of AuthDtos.FamilyMemberInput, staged here until the real account (and its family_members rows) get created on OTP verification. Null when the resident didn't add any family members. */
    @Column(name = "family_members_json")
    private String familyMembersJson;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getFlatNumber() {
        return flatNumber;
    }

    public void setFlatNumber(String flatNumber) {
        this.flatNumber = flatNumber;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getFamilyMembersJson() {
        return familyMembersJson;
    }

    public void setFamilyMembersJson(String familyMembersJson) {
        this.familyMembersJson = familyMembersJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
