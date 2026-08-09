package com.pavilion.api.entity;

import jakarta.persistence.*;
import java.time.Instant;

// Kept separate from AppEvent (rather than an event "category") since meetings only need to
// show when and where, not the richer festival/celebration presentation the Events page uses.
@Entity
@Table(name = "resident_meetings")
public class ResidentMeeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(name = "meeting_date", nullable = false)
    private Instant meetingDate;

    @Column(nullable = false)
    private String location;

    @Column
    private String notes;

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Instant getMeetingDate() {
        return meetingDate;
    }

    public void setMeetingDate(Instant meetingDate) {
        this.meetingDate = meetingDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
