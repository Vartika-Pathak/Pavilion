package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.Building;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.AuditLogRepository;
import com.pavilion.api.repository.BuildingRepository;
import com.pavilion.api.security.RecaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuditLogControllerTest extends AbstractIntegrationTest {

    @MockBean
    private RecaptchaService recaptchaService;

    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private BuildingRepository buildingRepository;

    @BeforeEach
    void stubCaptchaAsPassing() {
        when(recaptchaService.isConfigured()).thenReturn(true);
        when(recaptchaService.verify(any())).thenReturn(true);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/audit-logs")).andExpect(status().isUnauthorized());
    }

    @Test
    void residentCannotViewAuditLogs() throws Exception {
        User resident = createUser("resident");
        mockMvc.perform(get("/api/audit-logs").cookie(sessionCookie(resident)))
                .andExpect(status().isForbidden());
    }

    @Test
    void creatingAResourceWritesAnAuditLogEntryWithItsNameAsTheLabel() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/buildings")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Tower A\",\"totalFlats\":10}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/audit-logs").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adminName").value(admin.getName()))
                .andExpect(jsonPath("$[0].method").value("POST"))
                .andExpect(jsonPath("$[0].path").value("/api/buildings"))
                .andExpect(jsonPath("$[0].summary").value("Created buildings (Tower A)"));
    }

    @Test
    void deletingAResourceFallsBackToItsIdAsTheLabelSinceThereIsNoResponseBody() throws Exception {
        User admin = createUser("admin");
        Building building = new Building();
        building.setName("Tower A");
        building.setTotalFlats(10);
        building = buildingRepository.save(building);

        mockMvc.perform(delete("/api/buildings/" + building.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/audit-logs").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].summary").value("Deleted buildings (#" + building.getId() + ")"));
    }

    @Test
    void readOnlyRequestsDoNotCreateAuditLogEntries() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(get("/api/buildings").cookie(sessionCookie(admin))).andExpect(status().isOk());

        long countBefore = auditLogRepository.count();
        mockMvc.perform(get("/api/buildings").cookie(sessionCookie(admin))).andExpect(status().isOk());
        assertThat(auditLogRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void loginWithTheWrongPasswordDoesNotCreateAnAuditLogEntry() throws Exception {
        long countBefore = auditLogRepository.count();
        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"nobody@test.local\",\"password\":\"wrongpassword\"}"));
        assertThat(auditLogRepository.count()).isEqualTo(countBefore);
    }

    @Test
    void aSuccessfulLoginCreatesAnAuditLogEntryForAnyRole() throws Exception {
        User guard = createUser("guard");
        User admin = createUser("admin");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + guard.getEmail() + "\",\"password\":\"password123\",\"captchaToken\":\"tok\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/audit-logs").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adminName").value(guard.getName()))
                .andExpect(jsonPath("$[0].summary").value("Logged in"));
    }

    @Test
    void viewingACuratedPageCreatesAnAuditLogEntry() throws Exception {
        User resident = createUser("resident");
        User admin = createUser("admin");

        mockMvc.perform(get("/api/events").cookie(sessionCookie(resident))).andExpect(status().isOk());

        mockMvc.perform(get("/api/audit-logs").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].adminName").value(resident.getName()))
                .andExpect(jsonPath("$[0].summary").value("Viewed Events"));
    }
}
