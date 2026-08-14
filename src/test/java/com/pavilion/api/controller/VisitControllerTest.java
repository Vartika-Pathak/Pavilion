package com.pavilion.api.controller;

import com.jayway.jsonpath.JsonPath;
import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Visit;
import com.pavilion.api.repository.VisitRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Exercises exactly the authorization rules SecurityConfig/VisitController declare: any signed-in
// user can create/confirm/list their own visits, but only a guard or admin can look up an OTP or
// decide a visit (@PreAuthorize) — the case that was silently returning 500 instead of 403 before
// GlobalExceptionHandlerTest's regression fix.
class VisitControllerTest extends AbstractIntegrationTest {

    @Autowired
    private VisitRepository visitRepository;

    @Test
    void createRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/visits")
                        .contentType("application/json")
                        .content("""
                                {"visitType":"guest","visitorName":"Alex"}"""))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void residentCanCreateAndListTheirOwnVisits() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(post("/api/visits")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"visitType":"guest","visitorName":"Alex","visitorPhone":"9551234567"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.otpCode").isNotEmpty());

        mockMvc.perform(get("/api/visits/mine").cookie(sessionCookie(resident)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitorName").value("Alex"));
    }

    @Test
    void visitorNameRejectsDigitsAndSymbols() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(post("/api/visits")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"visitType":"guest","visitorName":"Alex99"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("visitorName: Visitor name can only contain letters and spaces"));
    }

    @Test
    void visitorPhoneMustBeExactlyTenDigitsWhenGiven() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(post("/api/visits")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"visitType":"guest","visitorName":"Alex","visitorPhone":"12345"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("visitorPhone: Mobile number must be exactly 10 digits, starting with 6-9"));

        mockMvc.perform(post("/api/visits")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"visitType":"guest","visitorName":"Alex","visitorPhone":"95512345a"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void visitorPhoneCannotStartWithZeroThroughFive() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(post("/api/visits")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"visitType":"guest","visitorName":"Alex","visitorPhone":"5551234567"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void visitorPhoneStaysOptional() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(post("/api/visits")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"visitType":"guest","visitorName":"Alex"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.visitorPhone").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void lookupRejectsAMalformedOtpBeforeEvenCheckingRole() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(post("/api/visits/lookup")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"otpCode":"abc"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("otpCode: Enter the 6-digit code"));
    }

    @Test
    void visitWithAnEmailAwaitsVerificationAndHidesTheOtpUntilConfirmed() throws Exception {
        User resident = createUser("resident");

        String created = mockMvc.perform(post("/api/visits")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"visitType":"guest","visitorName":"Alex","visitorEmail":"alex.visitor@test.local"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("awaiting_verification"))
                .andExpect(jsonPath("$.otpCode").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        Long visitId = ((Number) JsonPath.read(created, "$.id")).longValue();
        Visit visit = visitRepository.findById(visitId).orElseThrow();

        mockMvc.perform(post("/api/visits/" + visitId + "/confirm")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"otpCode\":\"" + visit.getOtpCode() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    void residentCannotConfirmAnotherResidentsVisit() throws Exception {
        User resident = createUser("resident");
        User otherResident = createUser("resident");

        String created = mockMvc.perform(post("/api/visits")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"visitType":"guest","visitorName":"Alex","visitorEmail":"alex.visitor2@test.local"}"""))
                .andReturn().getResponse().getContentAsString();
        Long visitId = ((Number) JsonPath.read(created, "$.id")).longValue();
        Visit visit = visitRepository.findById(visitId).orElseThrow();

        mockMvc.perform(post("/api/visits/" + visitId + "/confirm")
                        .cookie(sessionCookie(otherResident))
                        .contentType("application/json")
                        .content("{\"otpCode\":\"" + visit.getOtpCode() + "\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void residentIsForbiddenFromLookupAndDecide() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(post("/api/visits/lookup")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"otpCode":"123456"}"""))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("You don't have permission to do that"));

        mockMvc.perform(post("/api/visits/1/decide")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"approve":true}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    void guardCanLookUpAndApproveAPendingVisit() throws Exception {
        User resident = createUser("resident");
        User guard = createUser("guard");

        String created = mockMvc.perform(post("/api/visits")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"visitType":"guest","visitorName":"Alex","visitorPhone":"9551234567"}"""))
                .andReturn().getResponse().getContentAsString();
        String otpCode = JsonPath.read(created, "$.otpCode");
        Long visitId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(post("/api/visits/lookup")
                        .cookie(sessionCookie(guard))
                        .contentType("application/json")
                        .content("{\"otpCode\":\"" + otpCode + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.residentFlatNumber").value("A-1"));

        mockMvc.perform(post("/api/visits/" + visitId + "/decide")
                        .cookie(sessionCookie(guard))
                        .contentType("application/json")
                        .content("""
                                {"approve":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("approved"));

        assertThat(visitRepository.findById(visitId).orElseThrow().getApprovedBy().getId()).isEqualTo(guard.getId());
    }

    @Test
    void guardAndAdminCanListAllVisitsButResidentCannot() throws Exception {
        User resident = createUser("resident");
        User guard = createUser("guard");
        User admin = createUser("admin");

        mockMvc.perform(post("/api/visits")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"visitType":"maintenance_staff","visitorName":"Sam","visitorPhone":"9551234567"}"""))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/visits").cookie(sessionCookie(guard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitorName").value("Sam"))
                .andExpect(jsonPath("$[0].visitType").value("maintenance_staff"))
                .andExpect(jsonPath("$[0].residentFlatNumber").value("A-1"));

        mockMvc.perform(get("/api/visits").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].visitorName").value("Sam"));

        mockMvc.perform(get("/api/visits").cookie(sessionCookie(resident)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanAlsoDecideAVisit() throws Exception {
        User resident = createUser("resident");
        User admin = createUser("admin");

        String created = mockMvc.perform(post("/api/visits")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("""
                                {"visitType":"household_help","visitorName":"Jamie"}"""))
                .andReturn().getResponse().getContentAsString();
        Long visitId = ((Number) JsonPath.read(created, "$.id")).longValue();

        mockMvc.perform(post("/api/visits/" + visitId + "/decide")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("""
                                {"approve":false}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("denied"));
    }
}
