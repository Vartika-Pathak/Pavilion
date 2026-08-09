package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.User;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaintenanceRateControllerTest extends AbstractIntegrationTest {

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/maintenance-rates")).andExpect(status().isUnauthorized());
    }

    @Test
    void residentCannotUpdateRates() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(put("/api/maintenance-rates/2bhk")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"monthlyAmountPaise\":500000}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanSetAndListRates() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(put("/api/maintenance-rates/2bhk")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"monthlyAmountPaise\":500000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flatType").value("2bhk"))
                .andExpect(jsonPath("$.monthlyAmountPaise").value(500000));

        mockMvc.perform(get("/api/maintenance-rates").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flatType").value("2bhk"));
    }

    @Test
    void updatingAnUnknownFlatTypeFails() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(put("/api/maintenance-rates/studio")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"monthlyAmountPaise\":500000}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updatingTheSameFlatTypeTwiceUpsertsInsteadOfDuplicating() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(put("/api/maintenance-rates/2bhk")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"monthlyAmountPaise\":500000}"))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/maintenance-rates/2bhk")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"monthlyAmountPaise\":600000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthlyAmountPaise").value(600000));

        mockMvc.perform(get("/api/maintenance-rates").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
