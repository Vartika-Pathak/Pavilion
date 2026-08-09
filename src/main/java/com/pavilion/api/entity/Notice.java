package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

// Distinct from a news post — notices are short, operational, and can be pinned or set to
// auto-expire, more like a bulletin board than a blog feed.
@Entity
@Table(name = "notices")
public class Notice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String content;

    // One of "general", "maintenance", "event", "urgent".
    @Column(nullable = false)
    private String category = "general";

    // One of "low", "normal", "high".
    @Column(nullable = false)
    private String priority = "normal";

    @Column(nullable = false)
    private boolean pinned = false;

    @Column(name = "expires_at")
    private String expiresAt;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public boolean isPinned() {
        return pinned;
    }

    public void setPinned(boolean pinned) {
        this.pinned = pinned;
    }

    public String getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
