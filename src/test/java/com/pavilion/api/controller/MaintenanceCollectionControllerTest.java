package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.Building;
import com.pavilion.api.entity.Flat;
import com.pavilion.api.entity.MaintenanceRate;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.BuildingRepository;
import com.pavilion.api.repository.FlatRepository;
import com.pavilion.api.repository.MaintenanceRateRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaintenanceCollectionControllerTest extends AbstractIntegrationTest {

    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private FlatRepository flatRepository;
    @Autowired
    private MaintenanceRateRepository maintenanceRateRepository;

    private Flat createFlat(String flatNumber, String flatType) {
        Building building = new Building();
        building.setName("Tower A");
        building.setTotalFlats(10);
        building = buildingRepository.save(building);

        Flat flat = new Flat();
        flat.setBuildingId(building.getId());
        flat.setFlatNumber(flatNumber);
        flat.setFlatType(flatType);
        flat.setOccupied(true);
        flat.setOwnershipType("owner");
        return flatRepository.save(flat);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/maintenance-collections")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanRecordAndListCollections() throws Exception {
        User admin = createUser("admin");
        Flat flat = createFlat("A-1", "2bhk");

        mockMvc.perform(post("/api/maintenance-collections")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"flatId\":" + flat.getId() + ",\"payerName\":\"Resident A\",\"amountPaise\":500000,"
                                + "\"paymentDate\":\"2026-08-05\",\"paymentMode\":\"upi\",\"forMonth\":\"2026-08\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.buildingName").value("Tower A"))
                .andExpect(jsonPath("$.flatNumber").value("A-1"));

        mockMvc.perform(get("/api/maintenance-collections?flatId=" + flat.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].payerName").value("Resident A"));
    }

    @Test
    void recordingAgainstAnUnknownFlatFails() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(post("/api/maintenance-collections")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"flatId\":999999,\"payerName\":\"Resident A\",\"amountPaise\":500000,"
                                + "\"paymentDate\":\"2026-08-05\",\"paymentMode\":\"upi\",\"forMonth\":\"2026-08\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanDeleteACollection() throws Exception {
        User admin = createUser("admin");
        Flat flat = createFlat("A-1", "2bhk");

        String response = mockMvc.perform(post("/api/maintenance-collections")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"flatId\":" + flat.getId() + ",\"payerName\":\"Resident A\",\"amountPaise\":500000,"
                                + "\"paymentDate\":\"2026-08-05\",\"paymentMode\":\"upi\",\"forMonth\":\"2026-08\"}"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/maintenance-collections/" + id).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void backfillIsIdempotentAcrossRepeatedCalls() throws Exception {
        User admin = createUser("admin");
        createFlat("A-1", "2bhk");
        createFlat("A-2", "3bhk");

        MaintenanceRate rate2bhk = new MaintenanceRate();
        rate2bhk.setFlatType("2bhk");
        rate2bhk.setMonthlyAmountPaise(500000L);
        maintenanceRateRepository.save(rate2bhk);

        MaintenanceRate rate3bhk = new MaintenanceRate();
        rate3bhk.setFlatType("3bhk");
        rate3bhk.setMonthlyAmountPaise(700000L);
        maintenanceRateRepository.save(rate3bhk);

        String firstRun = mockMvc.perform(post("/api/maintenance-collections/backfill")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"months\":3}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int firstCreated = com.jayway.jsonpath.JsonPath.read(firstRun, "$.createdCount");

        String secondRun = mockMvc.perform(post("/api/maintenance-collections/backfill")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"months\":3}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        int secondCreated = com.jayway.jsonpath.JsonPath.read(secondRun, "$.createdCount");
        int secondSkipped = com.jayway.jsonpath.JsonPath.read(secondRun, "$.skippedCount");

        org.assertj.core.api.Assertions.assertThat(firstCreated).isGreaterThan(0);
        org.assertj.core.api.Assertions.assertThat(secondCreated).isZero();
        org.assertj.core.api.Assertions.assertThat(secondSkipped).isEqualTo(6);
    }

    @Test
    void backfillWithNoBodyDefaultsToThreeMonths() throws Exception {
        User admin = createUser("admin");
        createFlat("A-1", "2bhk");

        MaintenanceRate rate = new MaintenanceRate();
        rate.setFlatType("2bhk");
        rate.setMonthlyAmountPaise(500000L);
        maintenanceRateRepository.save(rate);

        mockMvc.perform(post("/api/maintenance-collections/backfill").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monthsBackfilled.length()").value(3));
    }
}
