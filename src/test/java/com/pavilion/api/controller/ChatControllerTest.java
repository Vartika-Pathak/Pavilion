package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.User;
import com.pavilion.api.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// ChatService talks to the real Gemini API, so it's mocked here — these tests are only about the
// controller's own behavior: who can reach it, and the IP-based rate limit. Each rate-limit-
// sensitive test uses its own X-Forwarded-For value so tests don't share the limiter's window
// (ChatRateLimiter is a real singleton bean, not reset between tests in the same class).
class ChatControllerTest extends AbstractIntegrationTest {

    @MockBean
    private ChatService chatService;

    @Test
    void suggestionsAreOpenToAnonymousVisitors() throws Exception {
        when(chatService.suggestionsFor(false)).thenReturn(List.of("What is Pavilion?"));

        mockMvc.perform(get("/api/chat/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0]").value("What is Pavilion?"));
    }

    @Test
    void suggestionsDifferForASignedInUser() throws Exception {
        User user = createUser("resident");
        when(chatService.suggestionsFor(true)).thenReturn(List.of("How do I raise an emergency alert?"));

        mockMvc.perform(get("/api/chat/suggestions").cookie(sessionCookie(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions[0]").value("How do I raise an emergency alert?"));
    }

    @Test
    void anonymousVisitorsCanSendAMessage() throws Exception {
        when(chatService.ask(eq("s1"), eq("hello"), isNull())).thenReturn("Hi there!");

        mockMvc.perform(post("/api/chat/message")
                        .header("X-Forwarded-For", "10.0.0.1")
                        .contentType("application/json")
                        .content("""
                                {"sessionId":"s1","message":"hello"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hi there!"));
    }

    @Test
    void signedInUserIsPassedToChatServiceSoRepliesCanBePersonalized() throws Exception {
        User user = createUser("resident");
        when(chatService.ask(eq("s2"), eq("hi"), any(User.class))).thenReturn("Hi, resident!");

        mockMvc.perform(post("/api/chat/message")
                        .cookie(sessionCookie(user))
                        .header("X-Forwarded-For", "10.0.0.2")
                        .contentType("application/json")
                        .content("""
                                {"sessionId":"s2","message":"hi"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("Hi, resident!"));
    }

    @Test
    void ninthMessageWithinAMinuteFromTheSameClientIsRateLimited() throws Exception {
        when(chatService.ask(anyString(), anyString(), any())).thenReturn("ok");

        for (int i = 0; i < 8; i++) {
            mockMvc.perform(post("/api/chat/message")
                            .header("X-Forwarded-For", "10.0.0.3")
                            .contentType("application/json")
                            .content("{\"sessionId\":\"rl\",\"message\":\"msg " + i + "\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/chat/message")
                        .header("X-Forwarded-For", "10.0.0.3")
                        .contentType("application/json")
                        .content("""
                                {"sessionId":"rl","message":"one too many"}"""))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error").isNotEmpty());
    }

    @Test
    void rateLimitIsPerClientNotGlobal() throws Exception {
        when(chatService.ask(anyString(), anyString(), any())).thenReturn("ok");

        for (int i = 0; i < 8; i++) {
            mockMvc.perform(post("/api/chat/message")
                            .header("X-Forwarded-For", "10.0.0.4")
                            .contentType("application/json")
                            .content("{\"sessionId\":\"rl2\",\"message\":\"msg " + i + "\"}"))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(post("/api/chat/message")
                        .header("X-Forwarded-For", "10.0.0.5")
                        .contentType("application/json")
                        .content("""
                                {"sessionId":"rl2","message":"from a different client"}"""))
                .andExpect(status().isOk());
    }
}
