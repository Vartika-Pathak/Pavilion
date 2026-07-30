package com.pavilion.api.controller;

import com.pavilion.api.dto.ChatDtos.ChatRequest;
import com.pavilion.api.dto.ChatDtos.ChatResponse;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.security.CurrentUserResolver;
import com.pavilion.api.service.ChatService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;
    private final CurrentUserResolver currentUserResolver;

    public ChatController(ChatService chatService, CurrentUserResolver currentUserResolver) {
        this.chatService = chatService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/message")
    public ChatResponse message(@Valid @RequestBody ChatRequest body, HttpServletRequest request) {
        currentUserResolver.resolve(request)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Not signed in"));

        String reply = chatService.ask(body.message());
        return new ChatResponse(reply);
    }
}
