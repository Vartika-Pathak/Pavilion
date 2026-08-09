package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

// A public directory/profile listing, distinct from the login "users" table — no write API
// exists for this in the Node version either, so it's read-only here too.
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "flat_number", nullable = false)
    private String flatNumber;

    @Column
    private String bio;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @Column(name = "joined_at", nullable = false)
    private Instant joinedAt = Instant.now();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFlatNumber() {
        return flatNumber;
    }

    public void setFlatNumber(String flatNumber) {
        this.flatNumber = flatNumber;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }
}
