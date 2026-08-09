package com.pavilion.api.amenities;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

public class AmenitySlots {

    public static final List<String> VALID_SLOTS = List.of("morning", "afternoon", "evening");

    // The hour (24h, server-local time) each slot ends at — used to decide whether a slot for a
    // given date has already passed. Keep in sync with slotLabels in the frontend's amenities page.
    private static final Map<String, Integer> SLOT_END_HOUR = Map.of(
            "morning", 12,
            "afternoon", 17,
            "evening", 21);

    private AmenitySlots() {
    }

    public static boolean isSlotPast(String bookingDate, String slot, Instant now) {
        LocalDate date = LocalDate.parse(bookingDate);
        Instant slotEnd = date.atStartOfDay(ZoneId.systemDefault())
                .withHour(SLOT_END_HOUR.get(slot))
                .toInstant();
        return !slotEnd.isAfter(now);
    }
}
