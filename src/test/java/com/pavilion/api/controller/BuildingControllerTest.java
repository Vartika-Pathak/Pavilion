package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.Building;
import com.pavilion.api.entity.Flat;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.BuildingRepository;
import com.pavilion.api.repository.FlatRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BuildingControllerTest extends AbstractIntegrationTest {

    @Autowired
    private BuildingRepository buildingRepository;

    @Autowired
    private FlatRepository flatRepository;

    private Building createBuilding(String name, int totalFlats) {
        Building building = new Building();
        building.setName(name);
        building.setTotalFlats(totalFlats);
        return buildingRepository.save(building);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/buildings")).andExpect(status().isUnauthorized());
    }

    @Test
    void residentCannotListBuildings() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(get("/api/buildings").cookie(sessionCookie(resident)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndListBuildings() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/buildings")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Tower A\",\"totalFlats\":20}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Tower A"))
                .andExpect(jsonPath("$.totalFlats").value(20));

        mockMvc.perform(get("/api/buildings").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Tower A"));
    }

    @Test
    void adminCanUpdateABuilding() throws Exception {
        User admin = createUser("admin");
        Building building = createBuilding("Tower A", 20);

        mockMvc.perform(put("/api/buildings/" + building.getId())
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Tower A Renamed\",\"totalFlats\":25}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Tower A Renamed"))
                .andExpect(jsonPath("$.totalFlats").value(25));
    }

    @Test
    void updatingAnUnknownBuildingReturns404() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(put("/api/buildings/999999")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Ghost Tower\",\"totalFlats\":5}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanDeleteAnEmptyBuilding() throws Exception {
        User admin = createUser("admin");
        Building building = createBuilding("Tower A", 20);

        mockMvc.perform(delete("/api/buildings/" + building.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void deletingABuildingWithFlatsFails() throws Exception {
        User admin = createUser("admin");
        Building building = createBuilding("Tower A", 20);
        Flat flat = new Flat();
        flat.setBuildingId(building.getId());
        flat.setFlatNumber("A-1");
        flat.setFlatType("2bhk");
        flat.setOccupied(true);
        flat.setOwnershipType("owner");
        flatRepository.save(flat);

        mockMvc.perform(delete("/api/buildings/" + building.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isConflict());
    }
}
