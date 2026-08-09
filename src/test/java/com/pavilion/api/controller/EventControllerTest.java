package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.AppEvent;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.AppEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventControllerTest extends AbstractIntegrationTest {

    @Autowired
    private AppEventRepository appEventRepository;

    private AppEvent createEvent(String title, Instant date) {
        AppEvent event = new AppEvent();
        event.setTitle(title);
        event.setEventDate(date);
        event.setLocation("Clubhouse");
        return appEventRepository.save(event);
    }

    @Test
    void listIsPublicWithNoAuth() throws Exception {
        createEvent("Diwali Mela", Instant.now().plus(10, ChronoUnit.DAYS));
        mockMvc.perform(get("/api/events"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Diwali Mela"));
    }

    @Test
    void upcomingOnlyReturnsFutureEvents() throws Exception {
        createEvent("Past Event", Instant.now().minus(10, ChronoUnit.DAYS));
        createEvent("Future Event", Instant.now().plus(10, ChronoUnit.DAYS));

        mockMvc.perform(get("/api/events/upcoming"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Future Event"));
    }

    @Test
    void getByIdReturns404ForUnknownEvent() throws Exception {
        mockMvc.perform(get("/api/events/999999")).andExpect(status().isNotFound());
    }

    @Test
    void creatingAnEventRequiresAdmin() throws Exception {
        mockMvc.perform(post("/api/events")
                        .contentType("application/json")
                        .content("{\"title\":\"Diwali Mela\",\"date\":\"2026-10-15T18:00:00Z\",\"location\":\"Clubhouse\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void residentCannotCreateAnEvent() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(post("/api/events")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"title\":\"Diwali Mela\",\"date\":\"2026-10-15T18:00:00Z\",\"location\":\"Clubhouse\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndDeleteAnEvent() throws Exception {
        User admin = createUser("admin");

        String response = mockMvc.perform(post("/api/events")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"title\":\"Diwali Mela\",\"date\":\"2026-10-15T18:00:00Z\",\"location\":\"Clubhouse\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Diwali Mela"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/events/" + id).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }
}
