package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.PendingSignup;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.PendingSignupRepository;
import com.pavilion.api.security.RecaptchaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest extends AbstractIntegrationTest {

    @MockBean
    private RecaptchaService recaptchaService;

    @Autowired
    private PendingSignupRepository pendingSignupRepository;

    @BeforeEach
    void stubCaptchaAsPassing() {
        when(recaptchaService.isConfigured()).thenReturn(true);
        when(recaptchaService.verify(any())).thenReturn(true);
    }

    @Test
    void signupStagesAnAccountWithoutCreatingTheRealUserYet() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Alex","email":"alex@test.local","flatNumber":"A-1",
                                 "password":"password123","captchaToken":"tok"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alex@test.local"))
                .andExpect(jsonPath("$.pendingSignupId").isNumber());

        assertThat(userRepository.findByEmail("alex@test.local")).isEmpty();
        assertThat(pendingSignupRepository.findByEmail("alex@test.local")).isPresent();
    }

    @Test
    void signupIsRejectedWhenCaptchaFailsVerification() throws Exception {
        when(recaptchaService.verify(any())).thenReturn(false);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Alex","email":"bad-captcha@test.local","flatNumber":"A-1",
                                 "password":"password123","captchaToken":"bad"}"""))
                .andExpect(status().isBadRequest());

        assertThat(pendingSignupRepository.findByEmail("bad-captcha@test.local")).isEmpty();
    }

    @Test
    void verifyingTheCorrectOtpCreatesTheUserAndSetsASessionCookie() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Sam","email":"sam@test.local","flatNumber":"B-2",
                                 "password":"password123","captchaToken":"tok"}"""))
                .andExpect(status().isOk());

        PendingSignup pending = pendingSignupRepository.findByEmail("sam@test.local").orElseThrow();

        mockMvc.perform(post("/api/auth/signup/verify")
                        .contentType("application/json")
                        .content("{\"pendingSignupId\":" + pending.getId() + ",\"otpCode\":\"" + pending.getOtpCode() + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("sam@test.local"))
                .andExpect(cookie().exists("session"));

        assertThat(userRepository.findByEmail("sam@test.local")).isPresent();
        assertThat(pendingSignupRepository.findByEmail("sam@test.local")).isEmpty();
    }

    @Test
    void verifyingTheWrongOtpFailsAndLeavesNoUserBehind() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Kim","email":"kim@test.local","flatNumber":"C-3",
                                 "password":"password123","captchaToken":"tok"}"""))
                .andExpect(status().isOk());

        PendingSignup pending = pendingSignupRepository.findByEmail("kim@test.local").orElseThrow();

        mockMvc.perform(post("/api/auth/signup/verify")
                        .contentType("application/json")
                        .content("{\"pendingSignupId\":" + pending.getId() + ",\"otpCode\":\"000000\"}"))
                .andExpect(status().isBadRequest());

        assertThat(userRepository.findByEmail("kim@test.local")).isEmpty();
    }

    @Test
    void loginWithTheWrongPasswordIsRejected() throws Exception {
        User user = createUser("resident");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"totally-wrong\",\"captchaToken\":\"tok\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginWithTheCorrectPasswordSucceedsAndSetsASessionCookie() throws Exception {
        User user = createUser("resident");

        mockMvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"" + user.getEmail() + "\",\"password\":\"password123\",\"captchaToken\":\"tok\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(cookie().exists("session"));
    }

    @Test
    void meIsRejectedWithoutASessionCookie() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Not signed in"));
    }

    @Test
    void meReturnsTheSignedInUser() throws Exception {
        User user = createUser("resident");

        mockMvc.perform(get("/api/auth/me").cookie(sessionCookie(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(user.getEmail()))
                .andExpect(jsonPath("$.role").value("resident"));
    }

    @Test
    void logoutClearsTheSessionCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(cookie().maxAge("session", 0));
    }
}
