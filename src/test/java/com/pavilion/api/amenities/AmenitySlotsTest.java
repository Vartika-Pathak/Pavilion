package com.pavilion.api.amenities;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;

class AmenitySlotsTest {

    @Test
    void aMorningSlotIsNotPastBeforeNoon() {
        LocalDate date = LocalDate.now().plusDays(1);
        Instant justBeforeNoon = date.atStartOfDay(ZoneId.systemDefault()).withHour(11).toInstant();

        assertThat(AmenitySlots.isSlotPast(date.toString(), "morning", justBeforeNoon)).isFalse();
    }

    @Test
    void aMorningSlotIsPastAtExactlyNoon() {
        LocalDate date = LocalDate.now().plusDays(1);
        Instant noon = date.atStartOfDay(ZoneId.systemDefault()).withHour(12).toInstant();

        assertThat(AmenitySlots.isSlotPast(date.toString(), "morning", noon)).isTrue();
    }

    @Test
    void anEveningSlotOnAFutureDateIsNotPast() {
        LocalDate date = LocalDate.now().plusDays(30);
        assertThat(AmenitySlots.isSlotPast(date.toString(), "evening", Instant.now())).isFalse();
    }

    @Test
    void anySlotOnAPastDateIsPast() {
        LocalDate date = LocalDate.now().minusDays(1);
        assertThat(AmenitySlots.isSlotPast(date.toString(), "evening", Instant.now())).isTrue();
    }
}
