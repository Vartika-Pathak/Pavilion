package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.ParkingPass;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.ParkingPassRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VehicleControllerTest extends AbstractIntegrationTest {

    private static final String VALID_BODY =
            "{\"plateNumber\":\"MH12AB1234\",\"vehicleType\":\"car\",\"ownerPhone\":\"9998887771\"}";

    @Autowired
    private ParkingPassRepository parkingPassRepository;

    private void givePassTo(User resident) {
        ParkingPass pass = new ParkingPass();
        pass.setFlatNumber(resident.getFlatNumber());
        pass.setPurchasedByResidentId(resident.getId());
        pass.setPurchasedByName(resident.getName());
        pass.setAmountPaidCents(500000);
        parkingPassRepository.save(pass);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/vehicles")).andExpect(status().isUnauthorized());
    }

    @Test
    void registeringWithoutAParkingPassFails() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content(VALID_BODY))
                .andExpect(status().isPaymentRequired());
    }

    @Test
    void residentCanRegisterAVehicle() throws Exception {
        User resident = createUser("resident");
        givePassTo(resident);

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
        givePassTo(resident);

        mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"plateNumber\":\"MH12AB1234\",\"vehicleType\":\"truck\",\"ownerPhone\":\"9998887771\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aBharatSeriesPlateNumberIsAccepted() throws Exception {
        User resident = createUser("resident");
        givePassTo(resident);

        mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"plateNumber\":\"22BH1234AB\",\"vehicleType\":\"car\",\"ownerPhone\":\"9998887771\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plateNumber").value("22BH1234AB"));
    }

    @Test
    void aPlateNumberWithSpacesAndLowercaseIsNormalized() throws Exception {
        User resident = createUser("resident");
        givePassTo(resident);

        mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"plateNumber\":\"mh 12-ab 1234\",\"vehicleType\":\"car\",\"ownerPhone\":\"9998887771\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.plateNumber").value("MH12AB1234"));
    }

    @Test
    void aPlateNumberWithSpecialCharactersFails() throws Exception {
        User resident = createUser("resident");
        givePassTo(resident);

        mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"plateNumber\":\"MH12@B1234\",\"vehicleType\":\"car\",\"ownerPhone\":\"9998887771\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aPlateNumberWithTheWrongShapeFails() throws Exception {
        User resident = createUser("resident");
        givePassTo(resident);

        mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"plateNumber\":\"MH121234\",\"vehicleType\":\"car\",\"ownerPhone\":\"9998887771\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anOwnerPhoneStartingWithZeroThroughFiveFails() throws Exception {
        User resident = createUser("resident");
        givePassTo(resident);

        mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"plateNumber\":\"MH12AB1234\",\"vehicleType\":\"car\",\"ownerPhone\":\"4998887771\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anOwnerPhoneWithTheWrongLengthFails() throws Exception {
        User resident = createUser("resident");
        givePassTo(resident);

        mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"plateNumber\":\"MH12AB1234\",\"vehicleType\":\"car\",\"ownerPhone\":\"99988877\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anOwnerPhoneWithSpacesIsNormalized() throws Exception {
        User resident = createUser("resident");
        givePassTo(resident);

        mockMvc.perform(post("/api/vehicles")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"plateNumber\":\"MH12AB1234\",\"vehicleType\":\"car\",\"ownerPhone\":\"99988 87771\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ownerPhone").value("9998887771"));
    }

    @Test
    void residentOnlySeesTheirOwnVehicles() throws Exception {
        User residentA = createUser("resident");
        User residentB = createUser("resident");
        givePassTo(residentA);

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
        // createUser() always uses flat "A-1", so this pass covers residentB too — matches the
        // "one purchase per flat" model.
        givePassTo(residentA);

        mockMvc.perform(post("/api/vehicles").cookie(sessionCookie(residentA)).contentType("application/json").content(VALID_BODY));
        mockMvc.perform(post("/api/vehicles").cookie(sessionCookie(residentB)).contentType("application/json").content(VALID_BODY));

        mockMvc.perform(get("/api/vehicles").cookie(sessionCookie(guard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void residentCanDeleteTheirOwnVehicle() throws Exception {
        User resident = createUser("resident");
        givePassTo(resident);
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
        givePassTo(residentA);
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
        givePassTo(resident);
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
