package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.ResidentMeeting;
import com.pavilion.api.repository.ResidentMeetingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ResidentMeetingControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ResidentMeetingRepository residentMeetingRepository;

    private ResidentMeeting createMeeting(String title, Instant date) {
        ResidentMeeting meeting = new ResidentMeeting();
        meeting.setTitle(title);
        meeting.setMeetingDate(date);
        meeting.setLocation("Clubhouse");
        return residentMeetingRepository.save(meeting);
    }

    @Test
    void onlyUpcomingMeetingsShowAndNoAuthIsRequired() throws Exception {
        createMeeting("Past AGM", Instant.now().minus(10, ChronoUnit.DAYS));
        createMeeting("Future AGM", Instant.now().plus(10, ChronoUnit.DAYS));

        mockMvc.perform(get("/api/resident-meetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Future AGM"));
    }
}
