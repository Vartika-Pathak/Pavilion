package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.User;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaintenanceSettingsControllerTest extends AbstractIntegrationTest {

    @Test
    void getRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/maintenance-settings")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminGetsDefaultsOnFirstRequest() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(get("/api/maintenance-settings").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDay").value(10))
                .andExpect(jsonPath("$.lateFeePercent").value(0));
    }

    @Test
    void adminCanUpdateSettings() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(put("/api/maintenance-settings")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"dueDay\":15,\"lateFeePercent\":5,\"openingBalanceNote\":\"Carried from FY25\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dueDay").value(15))
                .andExpect(jsonPath("$.lateFeePercent").value(5))
                .andExpect(jsonPath("$.openingBalanceNote").value("Carried from FY25"));
    }

    @Test
    void dueDayOutOfRangeFails() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(put("/api/maintenance-settings")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"dueDay\":31,\"lateFeePercent\":5,\"openingBalanceNote\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
