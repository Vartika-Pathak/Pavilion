package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.User;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehicleControllerTest extends AbstractIntegrationTest {

    private static final String VALID_BODY =
            "{\"plateNumber\":\"MH12AB1234\",\"vehicleType\":\"car\",\"ownerPhone\":\"9998887771\"}";

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/vehicles")).andExpect(status().isUnauthorized());
    }

    @Test
    void residentCanRegisterAVehicle() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plateNumber").value("MH12AB1234"))
                .andExpect(jsonPath("$.vehicleType").value("car"))
                .andExpect(jsonPath("$.ownerName").value(resident.getName()))
                .andExpect(jsonPath("$.flatNumber").value(resident.getFlatNumber()));
    }

    @Test
    void invalidVehicleTypeFails() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"plateNumber\":\"MH12AB1234\",\"vehicleType\":\"truck\",\"ownerPhone\":\"9998887771\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void residentOnlySeesTheirOwnVehicles() throws Exception {
        User residentA = createUser("resident");
        User residentB = createUser("resident");

        mockMvc.perform(post("/api/vehicles").cookie(sessionCookie(residentA)).contentType("application/json").content(VALID_BODY));
        mockMvc.perform(post("/api/vehicles").cookie(sessionCookie(residentB)).contentType("application/json").content(VALID_BODY));

        mockMvc.perform(get("/api/vehicles").cookie(sessionCookie(residentA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void guardSeesEveryVehicle() throws Exception {
        User residentA = createUser("resident");
        User residentB = createUser("resident");
        User guard = createUser("guard");

        mockMvc.perform(post("/api/vehicles").cookie(sessionCookie(residentA)).contentType("application/json").content(VALID_BODY));
        mockMvc.perform(post("/api/vehicles").cookie(sessionCookie(residentB)).contentType("application/json").content(VALID_BODY));

        mockMvc.perform(get("/api/vehicles").cookie(sessionCookie(guard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void residentCanDeleteTheirOwnVehicle() throws Exception {
        User resident = createUser("resident");
        String response = mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content(VALID_BODY))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/vehicles?id=" + id).cookie(sessionCookie(resident)))
                .andExpect(status().isNoContent());
    }

    @Test
    void residentCannotDeleteSomeoneElsesVehicle() throws Exception {
        User residentA = createUser("resident");
        User residentB = createUser("resident");
        String response = mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(residentA))
                        .contentType("application/json")
                        .content(VALID_BODY))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/vehicles?id=" + id).cookie(sessionCookie(residentB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanDeleteAnyVehicle() throws Exception {
        User resident = createUser("resident");
        User admin = createUser("admin");
        String response = mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content(VALID_BODY))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/vehicles?id=" + id).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }
}
