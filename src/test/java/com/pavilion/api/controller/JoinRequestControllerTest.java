package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class JoinRequestControllerTest extends AbstractIntegrationTest {

    @Test
    void anyoneCanSubmitAJoinRequest() throws Exception {
        mockMvc.perform(post("/api/join-requests")
                        .contentType("application/json")
                        .content("{\"name\":\"Alex\",\"email\":\"alex@test.local\",\"flatNumber\":\"A-101\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending"));
    }
}
