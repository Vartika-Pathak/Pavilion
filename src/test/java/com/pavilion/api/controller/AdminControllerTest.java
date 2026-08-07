package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.ResidentVerificationRequest;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.ResidentVerificationRequestRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest extends AbstractIntegrationTest {

    @Autowired
    private ResidentVerificationRequestRepository verificationRequestRepository;

    private ResidentVerificationRequest createRequest(String flatNumber, String name) {
        ResidentVerificationRequest request = new ResidentVerificationRequest();
        request.setFlatNumber(flatNumber);
        request.setName(name);
        return verificationRequestRepository.save(request);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/admin/verification-requests"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void residentCannotListVerificationRequests() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(get("/api/admin/verification-requests").cookie(sessionCookie(resident)))
                .andExpect(status().isForbidden());
    }

    @Test
    void guardCannotListVerificationRequests() throws Exception {
        User guard = createUser("guard");

        mockMvc.perform(get("/api/admin/verification-requests").cookie(sessionCookie(guard)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanListVerificationRequests() throws Exception {
        User admin = createUser("admin");
        createRequest("A-101", "Alex Sharma");

        mockMvc.perform(get("/api/admin/verification-requests").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flatNumber").value("A-101"))
                .andExpect(jsonPath("$[0].status").value("pending"));
    }

    @Test
    void adminCanToggleDocumentsAndPaymentFlags() throws Exception {
        User admin = createUser("admin");
        ResidentVerificationRequest request = createRequest("A-101", "Alex Sharma");

        mockMvc.perform(patch("/api/admin/verification-requests/" + request.getId())
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"documentsVerified\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentsVerified").value(true))
                .andExpect(jsonPath("$.paymentReceived").value(false))
                .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    void approvingFailsUntilBothDocumentsAndPaymentAreVerified() throws Exception {
        User admin = createUser("admin");
        ResidentVerificationRequest request = createRequest("A-101", "Alex Sharma");
        request.setDocumentsVerified(true);
        verificationRequestRepository.save(request);

        mockMvc.perform(patch("/api/admin/verification-requests/" + request.getId())
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"action\":\"approve\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Both documents and payment must be verified before approving"));
    }

    @Test
    void approvingSucceedsOnceBothAreVerified() throws Exception {
        User admin = createUser("admin");
        ResidentVerificationRequest request = createRequest("A-101", "Alex Sharma");
        request.setDocumentsVerified(true);
        request.setPaymentReceived(true);
        verificationRequestRepository.save(request);

        mockMvc.perform(patch("/api/admin/verification-requests/" + request.getId())
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"action\":\"approve\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));

        ResidentVerificationRequest saved = verificationRequestRepository.findById(request.getId()).orElseThrow();
        assertThat(saved.getReviewedBy()).isEqualTo(admin.getId());
        assertThat(saved.getReviewedAt()).isNotNull();
    }

    @Test
    void canApproveDocumentsAndPaymentInTheSameRequestAsTheApproveAction() throws Exception {
        User admin = createUser("admin");
        ResidentVerificationRequest request = createRequest("A-101", "Alex Sharma");

        mockMvc.perform(patch("/api/admin/verification-requests/" + request.getId())
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"documentsVerified\":true,\"paymentReceived\":true,\"action\":\"approve\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));
    }

    @Test
    void rejectingDoesNotRequireDocumentsOrPayment() throws Exception {
        User admin = createUser("admin");
        ResidentVerificationRequest request = createRequest("A-101", "Alex Sharma");

        mockMvc.perform(patch("/api/admin/verification-requests/" + request.getId())
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"action\":\"reject\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("rejected"));
    }

    @Test
    void updatingAMissingRequestReturnsNotFound() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(patch("/api/admin/verification-requests/999999")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"action\":\"reject\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void creatingAGuardRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/admin/guards")
                        .contentType("application/json")
                        .content("{\"name\":\"Sam Guard\",\"email\":\"sam@pavilion.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void residentCannotCreateAGuard() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(post("/api/admin/guards")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"name\":\"Sam Guard\",\"email\":\"sam@pavilion.com\",\"password\":\"password123\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanCreateAGuardAccount() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/admin/guards")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Sam Guard\",\"email\":\"sam@pavilion.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("sam@pavilion.com"))
                .andExpect(jsonPath("$.role").value("guard"));

        User guard = userRepository.findByEmail("sam@pavilion.com").orElseThrow();
        assertThat(guard.getRole()).isEqualTo("guard");
        assertThat(passwordEncoder.matches("password123", guard.getPasswordHash())).isTrue();
    }

    @Test
    void creatingAGuardRejectsANonPavilionEmail() throws Exception {
        User admin = createUser("admin");

        mockMvc.perform(post("/api/admin/guards")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Sam Guard\",\"email\":\"sam@gmail.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void creatingAGuardWithADuplicateEmailFails() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(post("/api/admin/guards")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Sam Guard\",\"email\":\"sam@pavilion.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/admin/guards")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"name\":\"Another Guard\",\"email\":\"sam@pavilion.com\",\"password\":\"password123\"}"))
                .andExpect(status().isConflict());
    }
}
