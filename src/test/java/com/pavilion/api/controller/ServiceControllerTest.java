package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.Service;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.ServiceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ServiceControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ServiceRepository serviceRepository;

    private Service createService(String name) {
        Service service = new Service();
        service.setName(name);
        service.setCategory("Plumber");
        service.setContactNumber("9876543210");
        return serviceRepository.save(service);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/services")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateListUpdateAndDeleteAService() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/services")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Ramesh Plumbing\",\"category\":\"Plumber\",\"contactNumber\":\"9876543210\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Ramesh Plumbing"));

        mockMvc.perform(get("/api/services").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("Plumber"));

        Service service = createService("Suresh Electric");
        mockMvc.perform(put("/api/services/" + service.getId())
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Suresh Electric\",\"category\":\"Electrician\",\"contactNumber\":\"9876543211\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.category").value("Electrician"));

        mockMvc.perform(delete("/api/services/" + service.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void creatingAServiceRejectsAContactNumberStartingWithZeroThroughFive() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/services")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Ramesh Plumbing\",\"category\":\"Plumber\",\"contactNumber\":\"0876543210\"}"))
                .andExpect(status().isBadRequest());
    }
}
