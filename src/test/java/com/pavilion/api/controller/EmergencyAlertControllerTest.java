package com.pavilion.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.User;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EmergencyAlertControllerTest extends AbstractIntegrationTest {

    @Test
    void raiseRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/emergency-alerts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void raisingTwiceForTheSameResidentReturnsTheSameActiveAlert() throws Exception {
        User resident = createUser("resident");

        String first = mockMvc.perform(post("/api/emergency-alerts").cookie(sessionCookie(resident)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String second = mockMvc.perform(post("/api/emergency-alerts").cookie(sessionCookie(resident)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        Long firstId = ((Number) JsonPath.read(first, "$.id")).longValue();
        Long secondId = ((Number) JsonPath.read(second, "$.id")).longValue();
        org.assertj.core.api.Assertions.assertThat(secondId).isEqualTo(firstId);
    }

    @Test
    void mineReturnsNoContentWhenThereIsNoActiveAlert() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(get("/api/emergency-alerts/mine").cookie(sessionCookie(resident)))
                .andExpect(status().isNoContent());
    }

    @Test
    void mineReturnsTheActiveAlertOnceRaised() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(post("/api/emergency-alerts").cookie(sessionCookie(resident)));

        mockMvc.perform(get("/api/emergency-alerts/mine").cookie(sessionCookie(resident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("active"));
    }

    @Test
    void anotherResidentCannotResolveSomeoneElsesAlert() throws Exception {
        User resident = createUser("resident");
        User otherResident = createUser("resident");

        String created = mockMvc.perform(post("/api/emergency-alerts").cookie(sessionCookie(resident)))
                .andReturn().getResponse().getContentAsString();
        Long alertId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(post("/api/emergency-alerts/" + alertId + "/resolve").cookie(sessionCookie(otherResident)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Only the reporting resident, a guard, or an admin can resolve this alert"));
    }

    @Test
    void theReportingResidentCanResolveTheirOwnAlert() throws Exception {
        User resident = createUser("resident");

        String created = mockMvc.perform(post("/api/emergency-alerts").cookie(sessionCookie(resident)))
                .andReturn().getResponse().getContentAsString();
        Long alertId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(post("/api/emergency-alerts/" + alertId + "/resolve").cookie(sessionCookie(resident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("resolved"));
    }

    @Test
    void guardCanResolveAnyResidentsAlert() throws Exception {
        User resident = createUser("resident");
        User guard = createUser("guard");

        String created = mockMvc.perform(post("/api/emergency-alerts").cookie(sessionCookie(resident)))
                .andReturn().getResponse().getContentAsString();
        Long alertId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(post("/api/emergency-alerts/" + alertId + "/resolve").cookie(sessionCookie(guard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("resolved"));
    }

    @Test
    void activeListsOnlyAlertsStillActive() throws Exception {
        User resident = createUser("resident");
        User guard = createUser("guard");

        String created = mockMvc.perform(post("/api/emergency-alerts").cookie(sessionCookie(resident)))
                .andReturn().getResponse().getContentAsString();
        Long alertId = ((Number) JsonPath.read(created, "$.id")).longValue();
        mockMvc.perform(post("/api/emergency-alerts/" + alertId + "/resolve").cookie(sessionCookie(guard)));

        User stillActiveResident = createUser("resident");
        mockMvc.perform(post("/api/emergency-alerts").cookie(sessionCookie(stillActiveResident)));

        mockMvc.perform(get("/api/emergency-alerts/active").cookie(sessionCookie(guard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("active"))))
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)));
    }
}
