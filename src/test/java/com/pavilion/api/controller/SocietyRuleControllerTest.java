package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.SocietyRule;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.SocietyRuleRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SocietyRuleControllerTest extends AbstractIntegrationTest {

    @Autowired
    private SocietyRuleRepository societyRuleRepository;

    private SocietyRule createRule(String title) {
        SocietyRule rule = new SocietyRule();
        rule.setTitle(title);
        rule.setDescription("Description");
        rule.setActive(true);
        return societyRuleRepository.save(rule);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/society-rules")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateListUpdateAndDeleteARule() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/society-rules")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"title\":\"No loud music after 10pm\",\"description\":\"Quiet hours\",\"active\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("No loud music after 10pm"));

        mockMvc.perform(get("/api/society-rules").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].active").value(true));

        SocietyRule rule = createRule("Parking rule");
        mockMvc.perform(put("/api/society-rules/" + rule.getId())
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"title\":\"Parking rule\",\"description\":\"Updated\",\"active\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(delete("/api/society-rules/" + rule.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }
}
