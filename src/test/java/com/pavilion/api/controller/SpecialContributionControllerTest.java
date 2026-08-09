package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.SpecialContribution;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.SpecialContributionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SpecialContributionControllerTest extends AbstractIntegrationTest {

    @Autowired
    private SpecialContributionRepository specialContributionRepository;

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/special-contributions")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateListAndDeleteAContribution() throws Exception {
        User admin = createUser("admin");

        String response = mockMvc.perform(post("/api/special-contributions")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"title\":\"Diwali Fund\",\"amountPaise\":500000,\"dueDate\":\"2026-10-15\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Diwali Fund"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/special-contributions").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Diwali Fund"));

        mockMvc.perform(delete("/api/special-contributions/" + id).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void negativeAmountFails() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(post("/api/special-contributions")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"title\":\"Diwali Fund\",\"amountPaise\":-1,\"dueDate\":\"2026-10-15\"}"))
                .andExpect(status().isBadRequest());
    }
}
