package com.pavilion.api.service;

import com.pavilion.api.entity.ChatMessage;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.ChatMessageRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

// Answers questions about using the Pavilion app via Google Gemini's
// free-tier API (generativelanguage.googleapis.com). Scoped with a system
// instruction to stay focused on the app itself rather than general chat.
// Conversation history is persisted per session so replies can reference
// earlier turns, instead of treating every message as a one-off.
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String GEMINI_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent";

    private static final List<String> LOGGED_OUT_SUGGESTIONS = List.of(
            "What is Pavilion?", "How do I sign up?", "How do I log in?", "How do I contact the committee?");

    private static final List<String> LOGGED_IN_SUGGESTIONS = List.of(
            "How do I log a visitor and get them an entry OTP?",
            "How do I raise an emergency alert?",
            "How do I report a maintenance issue?",
            "Who do I contact if my issue isn't resolved?",
            "How do I file a complaint?",
            "How do I book an amenity like the pool or clubhouse?",
            "What can I do from the Dashboard?");

    private static final String SYSTEM_INSTRUCTION = """
            You are Pavi, the help assistant embedded in the Pavilion app. Pavilion itself is a residential \
            society — a real building with residents, neighbors, shared amenities, and a managing committee — and \
            this app is simply how residents go about day-to-day life there. If asked what Pavilion is, describe \
            it as that community first (a place people live, with neighbors and a committee), not as a list of \
            app features. If asked your name, you're Pavi.

            The person asking may or may not be signed in yet — answer whichever of these is relevant to their \
            question:
            - Signing up: fill in the signup form, then enter the 6-digit code emailed to you to finish creating \
            the account.
            - Logging in: use the email and password from signup on the Log in page.
            - Logging a visitor (Entry page, once signed in): choose guest, cab/delivery, or household help, enter \
            the visitor's details, and optionally their email. If an email is given, an OTP is emailed to the \
            visitor and you must enter that same code back into the app to confirm the visit before it's usable \
            at the gate.
            - Emergency alerts (once signed in): a one-tap button that immediately notifies every neighbor, the \
            guard, and the admin. Only for real emergencies. The person who raised it, a guard, or an admin can \
            mark it resolved.
            - Maintenance requests (once signed in): report an issue (plumbing, electrical, appliance, \
            structural, or other), with a description and optional photos. Building staff update its status as \
            they work on it. If it's urgent, or it's been logged and nothing's happened, residents can also reach \
            the committee directly at committee@pavilion.example.com or +91 22 4589 6723 to chase it up.
            - Complaints (once signed in): raise a complaint (maintenance, security, noise, or other) with a \
            description, and track its status until it's resolved. The same direct contact applies if it needs \
            faster attention than the in-app status update.
            - Amenities (once signed in): book the clubhouse or swimming pool (free), or the tennis court or \
            party hall (paid, via card checkout), in morning, afternoon, or evening slots. A slot can't be booked \
            once it's already passed or already taken by someone else.
            - The Dashboard (once signed in) shows an overview based on the resident's role.
            - Residents can also check community events and resident meetings, read news, and browse the photo \
            gallery.
            - For anything outside what the app itself handles — billing questions, building policies, or \
            reaching a real person — the committee is at committee@pavilion.example.com or +91 22 4589 6723, also \
            listed on the Contact page.

            This app is for existing residents (and people actively signing up to become one) — not a place for \
            general property inquiries about buying or renting a unit, so there's no need to know unit prices or \
            floor plans in detail. For anything about actually living in Pavilion or using the app, even if it's \
            not one of the specifics above, give your best genuinely helpful answer using common sense about how \
            a residential society normally works, rather than deflecting. Only decline when the question needs \
            real facts you have no way of knowing (specific committee decisions, written policies, or anything \
            about buying/renting into the building) or is entirely unrelated to living in a residential \
            community — in that case, point them to the committee contact above.

            Keep answers short and practical. Write in plain prose only — never use markdown syntax (no asterisks, \
            no bullet points, no bold/italic markers). If you need to list steps, write them as plain sentences \
            like "First, ... Then, ...", not a formatted list.
            """;

    @Value("${gemini.api-key:}")
    private String apiKey;

    private final RestClient restClient = RestClient.create();
    private final ChatMessageRepository chatMessageRepository;

    public ChatService(ChatMessageRepository chatMessageRepository) {
        this.chatMessageRepository = chatMessageRepository;
    }

    @PostConstruct
    void init() {
        if (!isConfigured()) {
            log.warn("GEMINI_API_KEY is not set — the chat assistant will be unavailable until it's configured.");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    public List<String> suggestionsFor(boolean signedIn) {
        return signedIn ? LOGGED_IN_SUGGESTIONS : LOGGED_OUT_SUGGESTIONS;
    }

    public String ask(String sessionId, String message, User user) {
        if (!isConfigured()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "The chat assistant isn't configured yet");
        }

        List<ChatMessage> history = chatMessageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId);

        try {
            List<ContentTurn> turns = new ArrayList<>(history.stream()
                    .map(m -> new ContentTurn(geminiRole(m.getRole()), List.of(new Part(m.getContent()))))
                    .toList());
            turns.add(new ContentTurn("user", List.of(new Part(message))));

            GenerateRequest body = new GenerateRequest(new SystemInstruction(List.of(new Part(SYSTEM_INSTRUCTION))), turns);

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

            save(sessionId, user, "user", message);
            save(sessionId, user, "assistant", reply);

            return reply;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("Chat request failed", e);
            throw new ApiException(HttpStatus.BAD_GATEWAY, "The chat assistant is temporarily unavailable");
        }
    }

    private void save(String sessionId, User user, String role, String content) {
        ChatMessage entry = new ChatMessage();
        entry.setSessionId(sessionId);
        entry.setUser(user);
        entry.setRole(role);
        entry.setContent(content);
        chatMessageRepository.save(entry);
    }

    private static String geminiRole(String storedRole) {
        return "assistant".equals(storedRole) ? "model" : "user";
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

    private record ContentTurn(String role, List<Part> parts) {
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
