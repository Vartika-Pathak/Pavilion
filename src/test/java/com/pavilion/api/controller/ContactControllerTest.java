package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ContactControllerTest extends AbstractIntegrationTest {

    @Test
    void anyoneCanSubmitAContactMessage() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType("application/json")
                        .content("{\"name\":\"Alex\",\"email\":\"alex@test.local\",\"subject\":\"Question\",\"message\":\"Hello there\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subject").value("Question"));
    }

    @Test
    void blankMessageFails() throws Exception {
        mockMvc.perform(post("/api/contact")
                        .contentType("application/json")
                        .content("{\"name\":\"Alex\",\"email\":\"alex@test.local\",\"subject\":\"Question\",\"message\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
