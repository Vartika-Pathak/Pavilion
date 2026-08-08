package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Vendor;
import com.pavilion.api.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VendorControllerTest extends AbstractIntegrationTest {

    @Autowired
    private VendorRepository vendorRepository;

    private Vendor createVendor(String name) {
        Vendor vendor = new Vendor();
        vendor.setName(name);
        vendor.setContactPersonName("Contact Person");
        vendor.setContactNumber("9876543210");
        return vendorRepository.save(vendor);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/vendors")).andExpect(status().isUnauthorized());
    }

    @Test
    void residentCannotListVendors() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(get("/api/vendors").cookie(sessionCookie(resident)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAndListVendors() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/vendors")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"ABC Housekeeping\",\"contactPersonName\":\"Ravi Kumar\",\"contactNumber\":\"9876543210\",\"openingBalancePaise\":50000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("ABC Housekeeping"))
                .andExpect(jsonPath("$.openingBalancePaise").value(50000));

        mockMvc.perform(get("/api/vendors").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("ABC Housekeeping"));
    }

    @Test
    void openingBalanceDefaultsToZeroWhenOmitted() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/vendors")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"ABC Housekeeping\",\"contactPersonName\":\"Ravi Kumar\",\"contactNumber\":\"9876543210\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.openingBalancePaise").value(0));
    }

    @Test
    void adminCanUpdateAndDeleteAVendor() throws Exception {
        User admin = createUser("admin");
        Vendor vendor = createVendor("ABC Housekeeping");

        mockMvc.perform(put("/api/vendors/" + vendor.getId())
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"XYZ Housekeeping\",\"contactPersonName\":\"Ravi Kumar\",\"contactNumber\":\"9876543210\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("XYZ Housekeeping"));

        mockMvc.perform(delete("/api/vendors/" + vendor.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }
}
