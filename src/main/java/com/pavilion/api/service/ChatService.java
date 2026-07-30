package com.pavilion.api.service;

import com.pavilion.api.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

// Answers resident questions about using the Pavilion app via Google Gemini's
// free-tier API (generativelanguage.googleapis.com). Scoped with a system
// instruction to stay focused on the app itself rather than general chat.
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    private static final String SYSTEM_INSTRUCTION = """
            You are the help assistant embedded in Pavilion, a residential society management app. \
            Answer resident questions about how to use the app's features:
            - Signing up: fill in the signup form, then enter the 6-digit code emailed to you to finish creating the account.
            - Logging a visitor (Entry page): choose guest, cab/delivery, or household help, enter the visitor's \
            details, and optionally their email. If an email is given, an OTP is emailed to the visitor and you must \
            enter that same code back into the app to confirm the visit before it's usable at the gate.
            - Emergency alerts: a one-tap button that immediately notifies every neighbor, the guard, and the admin. \
            Only for real emergencies. The person who raised it, a guard, or an admin can mark it resolved.
            - The Dashboard shows an overview based on the resident's role.

            Keep answers short and practical. If asked something unrelated to using this app (general knowledge, \
            other topics), politely say you can only help with questions about the Pavilion app and suggest \
            contacting building management for anything else.
            """;

    @Value("${gemini.api-key:}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();

    @PostConstruct
    void init() {
        if (!isConfigured()) {
            log.warn("GEMINI_API_KEY is not set — the chat assistant will be unavailable until it's configured.");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String ask(String message) {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The chat assistant isn't configured yet");
        }

        try {
            GenerateRequest body = new GenerateRequest(
                    new SystemInstruction(List.of(new Part(SYSTEM_INSTRUCTION))),
                    List.of(new ContentTurn(List.of(new Part(message)))));

            GeminiResponse response = restClient.post()
                    .uri(GEMINI_URL)
                    .header("x-goog-api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(GeminiResponse.class);

            String reply = response == null ? null : extractText(response);
            if (reply == null || reply.isBlank()) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "The chat assistant didn't return a response");
            }
            return reply;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Chat request failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The chat assistant is temporarily unavailable");
        }
    }

    private static String extractText(GeminiResponse response) {
        if (response.candidates() == null || response.candidates().isEmpty()) {
            return null;
        }
        Content content = response.candidates().get(0).content();
        if (content == null || content.parts() == null || content.parts().isEmpty()) {
            return null;
        }
        return content.parts().get(0).text();
    }

    private record GenerateRequest(SystemInstruction systemInstruction, List<ContentTurn> contents) {
    }

    private record SystemInstruction(List<Part> parts) {
    }

    private record ContentTurn(List<Part> parts) {
    }

    private record Part(String text) {
    }

    private record GeminiResponse(List<Candidate> candidates) {
    }

    private record Candidate(Content content) {
    }

    private record Content(List<Part> parts) {
    }
}
