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
                                {"name":"Alex","email":"alex@test.local","flatNumber":"A1",
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
                                {"name":"Alex","email":"bad-captcha@test.local","flatNumber":"A1",
                                 "password":"password123","captchaToken":"bad"}"""))
                .andExpect(status().isBadRequest());

        assertThat(pendingSignupRepository.findByEmail("bad-captcha@test.local")).isEmpty();
    }

    @Test
    void signupRejectsANameContainingDigitsOrSymbols() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Alex99","email":"digits-in-name@test.local","flatNumber":"A1",
                                 "password":"password123","captchaToken":"tok"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("name: Name can only contain letters and spaces"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Alex@!","email":"symbols-in-name@test.local","flatNumber":"A1",
                                 "password":"password123","captchaToken":"tok"}"""))
                .andExpect(status().isBadRequest());

        assertThat(pendingSignupRepository.findByEmail("digits-in-name@test.local")).isEmpty();
    }

    @Test
    void signupRejectsAFlatNumberWithDisallowedCharacters() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Alex","email":"bad-flat@test.local","flatNumber":"@@@###",
                                 "password":"password123","captchaToken":"tok"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("Flat number")));
    }

    @Test
    void signupAcceptsAFlatNumberThatsALetterFollowedByUpToThreeDigits() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Alex","email":"ok-flat@test.local","flatNumber":"A101",
                                 "password":"password123","captchaToken":"tok"}"""))
                .andExpect(status().isOk());
    }

    @Test
    void signupRejectsAFlatNumberWithMoreThanThreeDigitsOrNoLetter() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Alex","email":"too-many-digits@test.local","flatNumber":"A1234",
                                 "password":"password123","captchaToken":"tok"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("flatNumber: Flat number must be a letter followed by 1-3 digits, e.g. A101"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Alex","email":"no-letter@test.local","flatNumber":"101",
                                 "password":"password123","captchaToken":"tok"}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    void signupRejectsAPasswordLongerThanBcryptsSeventyTwoByteLimit() throws Exception {
        String tooLong = "a".repeat(80);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Alex","email":"long-password@test.local","flatNumber":"A1",
                                 "password":"%s","captchaToken":"tok"}""".formatted(tooLong)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("password: Password must be between 8 and 72 characters"));
    }

    @Test
    void verifyingTheCorrectOtpCreatesTheUserAndSetsASessionCookie() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType("application/json")
                        .content("""
                                {"name":"Sam","email":"sam@test.local","flatNumber":"B2",
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
                                {"name":"Kim","email":"kim@test.local","flatNumber":"C3",
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
