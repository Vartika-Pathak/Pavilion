package com.pavilion.api.controller;

import com.pavilion.api.dto.ChatDtos.ChatRequest;
import com.pavilion.api.dto.ChatDtos.ChatResponse;
import com.pavilion.api.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Open to signed-out visitors too (e.g. "how do I sign up?" on the home
// page), unlike every other endpoint in this app — see ChatWidget.tsx for
// how the frontend varies its suggested questions by auth state instead.
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/message")
    public ChatResponse message(@Valid @RequestBody ChatRequest body) {
        String reply = chatService.ask(body.message());
        return new ChatResponse(reply);
    }
}
