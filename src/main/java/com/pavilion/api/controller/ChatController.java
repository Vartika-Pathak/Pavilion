package com.pavilion.api.controller;

import com.pavilion.api.dto.ChatDtos.ChatRequest;
import com.pavilion.api.dto.ChatDtos.ChatResponse;
import com.pavilion.api.dto.ChatDtos.SuggestionsResponse;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.security.CurrentUserResolver;
import com.pavilion.api.service.ChatRateLimiter;
import com.pavilion.api.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

// Open to signed-out visitors too (e.g. "how do I sign up?" on the home
// page), unlike every other endpoint in this app — see ChatWidget.tsx for
// how the frontend varies its suggested questions by auth state instead.
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final ChatRateLimiter rateLimiter;
    private final CurrentUserResolver currentUserResolver;

    public ChatController(
            ChatService chatService, ChatRateLimiter rateLimiter, CurrentUserResolver currentUserResolver) {
        this.chatService = chatService;
        this.rateLimiter = rateLimiter;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/suggestions")
    public SuggestionsResponse suggestions(HttpServletRequest request) {
        boolean signedIn = currentUserResolver.resolve(request).isPresent();
        return new SuggestionsResponse(chatService.suggestionsFor(signedIn));
    }

    @PostMapping("/message")
    public ChatResponse message(@Valid @RequestBody ChatRequest body, HttpServletRequest request) {
        if (!rateLimiter.allow(clientKey(request))) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "You're sending messages too quickly — please wait a moment.");
        }

        User user = currentUserResolver.resolve(request).orElse(null);
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
