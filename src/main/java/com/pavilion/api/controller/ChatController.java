package com.pavilion.api.controller;

import com.pavilion.api.dto.ChatDtos.ChatRequest;
import com.pavilion.api.dto.ChatDtos.ChatResponse;
import com.pavilion.api.dto.ChatDtos.SuggestionsResponse;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.service.ChatRateLimiter;
import com.pavilion.api.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

// Open to signed-out visitors too (e.g. "how do I sign up?" on the home
// page), unlike every other endpoint in this app — see SecurityConfig's
// permitAll list and ChatWidget.tsx for how the frontend varies its
// suggested questions by auth state instead.
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatRateLimiter rateLimiter;

    public ChatController(ChatService chatService, ChatRateLimiter rateLimiter) {
        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
    }

    @GetMapping("/suggestions")
    public SuggestionsResponse suggestions(@AuthenticationPrincipal User user) {
        return new SuggestionsResponse(chatService.suggestionsFor(user != null));
    }

    @PostMapping("/message")
    public ChatResponse message(
            @Valid @RequestBody ChatRequest body, @AuthenticationPrincipal User user, HttpServletRequest request) {
        if (!rateLimiter.allow(clientKey(request))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "You're sending messages too quickly — please wait a moment.");
        }

        String reply = chatService.ask(body.sessionId(), body.message(), user);
        return new ChatResponse(reply);
    }

    private static String clientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
