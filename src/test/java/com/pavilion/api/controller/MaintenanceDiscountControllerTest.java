package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.MaintenanceDiscount;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.MaintenanceDiscountRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaintenanceDiscountControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MaintenanceDiscountRepository maintenanceDiscountRepository;

    private MaintenanceDiscount createDiscount(String name) {
        MaintenanceDiscount discount = new MaintenanceDiscount();
        discount.setName(name);
        discount.setDiscountType("percent");
        discount.setValue(10L);
        discount.setActive(true);
        return maintenanceDiscountRepository.save(discount);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/maintenance-discounts")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateAndListDiscounts() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/maintenance-discounts")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Senior Citizen\",\"discountType\":\"percent\",\"value\":10,\"active\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Senior Citizen"))
                .andExpect(jsonPath("$.discountType").value("percent"));

        mockMvc.perform(get("/api/maintenance-discounts").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Senior Citizen"));
    }

    @Test
    void invalidDiscountTypeFails() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(post("/api/maintenance-discounts")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Senior Citizen\",\"discountType\":\"coupon\",\"value\":10,\"active\":true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanUpdateAndDeleteADiscount() throws Exception {
        User admin = createUser("admin");
        MaintenanceDiscount discount = createDiscount("Senior Citizen");

        mockMvc.perform(put("/api/maintenance-discounts/" + discount.getId())
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Senior Citizen\",\"discountType\":\"fixed\",\"value\":50000,\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.discountType").value("fixed"))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/maintenance-discounts/" + discount.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }
}
