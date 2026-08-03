package com.pavilion.api.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public class ChatDtos {

    public record ChatRequest(@NotBlank String sessionId, @NotBlank String message) {
    }

    public record ChatResponse(String reply) {
    }

    public record SuggestionsResponse(List<String> suggestions) {
    }
}
