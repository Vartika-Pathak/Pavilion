package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

// Named AppEvent (table app_events) rather than Event/events to steer clear of any
// collision with the SQL "EVENT" keyword used by MySQL's event scheduler.
@Entity
@Table(name = "app_events")
public class AppEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Column(name = "event_date", nullable = false)
    private Instant eventDate;

    @Column(nullable = false)
    private String location;

    @Column
    private String organizer;

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

    public Instant getEventDate() {
        return eventDate;
    }

    public void setEventDate(Instant eventDate) {
        this.eventDate = eventDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getOrganizer() {
        return organizer;
    }

    public void setOrganizer(String organizer) {
        this.organizer = organizer;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
