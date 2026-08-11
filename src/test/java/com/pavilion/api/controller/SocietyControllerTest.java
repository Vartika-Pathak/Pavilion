package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.User;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SocietyControllerTest extends AbstractIntegrationTest {

    @Test
    void getRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/society-info")).andExpect(status().isUnauthorized());
    }

    @Test
    void residentCannotViewSocietyInfo() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(get("/api/society-info").cookie(sessionCookie(resident)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminGetsBlankDefaultOnFirstRequest() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(get("/api/society-info").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(""));
    }

    @Test
    void adminCanUpdateSocietyInfo() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(put("/api/society-info")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Green Meadows\",\"address\":\"123 Main St\",\"contactNumber\":\"9876543210\",\"email\":\"office@greenmeadows.test\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Green Meadows"))
                .andExpect(jsonPath("$.email").value("office@greenmeadows.test"));

        mockMvc.perform(get("/api/society-info").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Green Meadows"));
    }

    @Test
    void updateRejectsBlankFields() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(put("/api/society-info")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"\",\"address\":\"123 Main St\",\"contactNumber\":\"9876543210\",\"email\":\"office@test.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateRejectsAContactNumberStartingWithZeroThroughFive() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(put("/api/society-info")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Green Meadows\",\"address\":\"123 Main St\",\"contactNumber\":\"5876543210\",\"email\":\"office@test.com\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateNormalizesASpacedOutContactNumber() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(put("/api/society-info")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Green Meadows\",\"address\":\"123 Main St\",\"contactNumber\":\"98765 43210\",\"email\":\"office@test.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contactNumber").value("9876543210"));
    }
}
