package com.pavilion.api.controller;

import com.pavilion.api.dto.EventDtos.EventRequest;
import com.pavilion.api.dto.EventDtos.EventResponse;
import com.pavilion.api.entity.AppEvent;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.AppEventRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

// Reads are public (permitAll in SecurityConfig) — anyone can browse the community calendar.
// Writes are admin-only: the Node version of this endpoint had no auth check at all, which
// looks like an oversight rather than an intentional public-write API, so this port closes
// that gap instead of reproducing it.
@RestController
@RequestMapping("/api/events")
public class EventController {

    private final AppEventRepository appEventRepository;

    public EventController(AppEventRepository appEventRepository) {
        this.appEventRepository = appEventRepository;
    }

    @GetMapping("/upcoming")
    public List<EventResponse> listUpcomingEvents() {
        return appEventRepository.findTop5ByEventDateGreaterThanEqualOrderByEventDateAsc(Instant.now())
                .stream().map(EventResponse::from).toList();
    }

    @GetMapping
    public List<EventResponse> listEvents() {
        return appEventRepository.findAllByOrderByEventDateAsc().stream().map(EventResponse::from).toList();
    }

    @GetMapping("/{id}")
    public EventResponse getEvent(@PathVariable Long id) {
        return appEventRepository.findById(id)
                .map(EventResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Event not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EventResponse> createEvent(@Valid @RequestBody EventRequest body) {
        AppEvent event = new AppEvent();
        event.setTitle(body.title());
        event.setDescription(body.description());
        event.setEventDate(body.date());
        event.setLocation(body.location());
        event.setOrganizer(body.organizer());
        event = appEventRepository.save(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(EventResponse.from(event));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable Long id) {
        if (!appEventRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Event not found");
        }
        appEventRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
