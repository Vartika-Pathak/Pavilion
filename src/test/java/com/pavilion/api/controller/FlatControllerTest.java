package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.Building;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.BuildingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FlatControllerTest extends AbstractIntegrationTest {

    @Autowired
    private BuildingRepository buildingRepository;

    private Building createBuilding(String name) {
        Building building = new Building();
        building.setName(name);
        building.setTotalFlats(10);
        return buildingRepository.save(building);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/flats")).andExpect(status().isUnauthorized());
    }

    @Test
    void residentCannotListFlats() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(get("/api/flats").cookie(sessionCookie(resident)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndListFlats() throws Exception {
        User admin = createUser("admin");
        Building building = createBuilding("Tower A");

        mockMvc.perform(post("/api/flats")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"buildingId\":" + building.getId() + ",\"flatNumber\":\"A-1\",\"flatType\":\"2bhk\",\"occupied\":true,\"ownershipType\":\"owner\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.flatNumber").value("A-1"))
                .andExpect(jsonPath("$.buildingName").value("Tower A"))
                .andExpect(jsonPath("$.flatType").value("2bhk"));

        mockMvc.perform(get("/api/flats").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flatNumber").value("A-1"));
    }

    @Test
    void creatingAFlatForAnUnknownBuildingReturns404() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/flats")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"buildingId\":999999,\"flatNumber\":\"A-1\",\"flatType\":\"2bhk\",\"occupied\":true,\"ownershipType\":\"owner\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingAFlatWithAnInvalidFlatTypeFails() throws Exception {
        User admin = createUser("admin");
        Building building = createBuilding("Tower A");

        mockMvc.perform(post("/api/flats")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"buildingId\":" + building.getId() + ",\"flatNumber\":\"A-1\",\"flatType\":\"studio\",\"occupied\":true,\"ownershipType\":\"owner\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanDeleteAFlat() throws Exception {
        User admin = createUser("admin");
        Building building = createBuilding("Tower A");

        String response = mockMvc.perform(post("/api/flats")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"buildingId\":" + building.getId() + ",\"flatNumber\":\"A-1\",\"flatType\":\"2bhk\",\"occupied\":true,\"ownershipType\":\"owner\"}"))
                .andReturn().getResponse().getContentAsString();
        Number flatId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/flats/" + flatId).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void adminCanAssignAResidentToAFlat() throws Exception {
        User admin = createUser("admin");
        User resident = createUser("resident");
        Building building = createBuilding("Tower A");

        mockMvc.perform(post("/api/flats")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"buildingId\":" + building.getId() + ",\"flatNumber\":\"A-1\",\"flatType\":\"2bhk\",\"occupied\":true,"
                                + "\"ownershipType\":\"owner\",\"residentId\":" + resident.getId() + "}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.residentId").value(resident.getId()))
                .andExpect(jsonPath("$.residentName").value(resident.getName()));
    }

    @Test
    void assigningANonResidentAccountFails() throws Exception {
        User admin = createUser("admin");
        User guard = createUser("guard");
        Building building = createBuilding("Tower A");

        mockMvc.perform(post("/api/flats")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"buildingId\":" + building.getId() + ",\"flatNumber\":\"A-1\",\"flatType\":\"2bhk\",\"occupied\":true,"
                                + "\"ownershipType\":\"owner\",\"residentId\":" + guard.getId() + "}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void guardCanReadTheDirectoryButNotWrite() throws Exception {
        User admin = createUser("admin");
        User guard = createUser("guard");
        User resident = createUser("resident");
        Building building = createBuilding("Tower A");

        mockMvc.perform(post("/api/flats")
                .cookie(sessionCookie(admin))
                .contentType("application/json")
                .content("{\"buildingId\":" + building.getId() + ",\"flatNumber\":\"A-1\",\"flatType\":\"2bhk\",\"occupied\":true,"
                        + "\"ownershipType\":\"owner\",\"residentId\":" + resident.getId() + "}"));

        mockMvc.perform(get("/api/flats/directory").cookie(sessionCookie(guard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].residentName").value(resident.getName()));

        mockMvc.perform(post("/api/flats")
                        .cookie(sessionCookie(guard))
                        .contentType("application/json")
                        .content("{\"buildingId\":" + building.getId() + ",\"flatNumber\":\"A-2\",\"flatType\":\"2bhk\",\"occupied\":false,\"ownershipType\":\"owner\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void residentSeesTheirOwnFlatOnlyOnceAssigned() throws Exception {
        User admin = createUser("admin");
        User resident = createUser("resident");
        Building building = createBuilding("Tower A");

        mockMvc.perform(get("/api/flats/mine").cookie(sessionCookie(resident)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/flats")
                .cookie(sessionCookie(admin))
                .contentType("application/json")
                .content("{\"buildingId\":" + building.getId() + ",\"flatNumber\":\"A-1\",\"flatType\":\"2bhk\",\"occupied\":true,"
                        + "\"ownershipType\":\"owner\",\"residentId\":" + resident.getId() + "}"));

        mockMvc.perform(get("/api/flats/mine").cookie(sessionCookie(resident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.flatNumber").value("A-1"));
    }

    @Test
    void residentCanRequestAChangeOnlyForTheirOwnFlat() throws Exception {
        User admin = createUser("admin");
        User resident = createUser("resident");
        User otherResident = createUser("resident");
        Building building = createBuilding("Tower A");

        String response = mockMvc.perform(post("/api/flats")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"buildingId\":" + building.getId() + ",\"flatNumber\":\"A-1\",\"flatType\":\"2bhk\",\"occupied\":true,"
                                + "\"ownershipType\":\"owner\",\"residentId\":" + resident.getId() + "}"))
                .andReturn().getResponse().getContentAsString();
        Number flatId = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(post("/api/flats/" + flatId + "/change-requests")
                        .cookie(sessionCookie(otherResident))
                        .contentType("application/json")
                        .content("{\"message\":\"Wrong flat type\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/flats/" + flatId + "/change-requests")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"message\":\"Wrong flat type, should be 3bhk\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("pending"));

        mockMvc.perform(get("/api/flats/change-requests").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].message").value("Wrong flat type, should be 3bhk"));
    }
}
