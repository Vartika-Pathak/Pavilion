package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.User;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ComplaintControllerTest extends AbstractIntegrationTest {

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/complaints")).andExpect(status().isUnauthorized());
    }

    @Test
    void residentOnlySeesTheirOwnComplaints() throws Exception {
        User residentA = createUser("resident");
        User residentB = createUser("resident");

        mockMvc.perform(post("/api/complaints")
                .cookie(sessionCookie(residentA))
                .contentType("application/json")
                .content("{\"category\":\"noise\",\"description\":\"Loud music at night\"}"));
        mockMvc.perform(post("/api/complaints")
                .cookie(sessionCookie(residentB))
                .contentType("application/json")
                .content("{\"category\":\"security\",\"description\":\"Gate left open\"}"));

        mockMvc.perform(get("/api/complaints").cookie(sessionCookie(residentA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("noise"));
    }

    @Test
    void guardSeesAllComplaints() throws Exception {
        User resident = createUser("resident");
        User guard = createUser("guard");

        mockMvc.perform(post("/api/complaints")
                .cookie(sessionCookie(resident))
                .contentType("application/json")
                .content("{\"category\":\"noise\",\"description\":\"Loud music at night\"}"));

        mockMvc.perform(get("/api/complaints").cookie(sessionCookie(guard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void residentCannotUpdateStatus() throws Exception {
        User resident = createUser("resident");
        String response = mockMvc.perform(post("/api/complaints")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"category\":\"noise\",\"description\":\"Loud music\"}"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(post("/api/complaints/" + id + "/status")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"status\":\"resolved\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUpdateStatusAndResolutionNote() throws Exception {
        User resident = createUser("resident");
        User admin = createUser("admin");
        String response = mockMvc.perform(post("/api/complaints")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"category\":\"noise\",\"description\":\"Loud music\"}"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(post("/api/complaints/" + id + "/status")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"status\":\"resolved\",\"resolutionNote\":\"Spoke with the resident\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("resolved"))
                .andExpect(jsonPath("$.resolutionNote").value("Spoke with the resident"));
    }
}
