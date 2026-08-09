package com.pavilion.api.dto;

import com.pavilion.api.entity.AppEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public class EventDtos {

    public record EventResponse(
            Long id, String title, String description, Instant date, String location, String organizer, Instant createdAt) {
        public static EventResponse from(AppEvent event) {
            return new EventResponse(
                    event.getId(), event.getTitle(), event.getDescription(), event.getEventDate(),
                    event.getLocation(), event.getOrganizer(), event.getCreatedAt());
        }
    }

    public record EventRequest(
            @NotBlank(message = "Title is required") String title,
            String description,
            @NotNull(message = "Date is required") Instant date,
            @NotBlank(message = "Location is required") String location,
            String organizer) {
    }
}
