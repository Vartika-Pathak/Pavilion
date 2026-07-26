package com.pavilion.api.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

// Sends over Resend's HTTPS API rather than raw SMTP — many free hosting
// tiers (Render included) block outbound SMTP ports to prevent spam abuse,
// but plain HTTPS always goes through.
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String RESEND_URL = "https://api.resend.com/emails";

    @Value("${resend.api-key:}")
    private String apiKey;

    @Value("${resend.from:onboarding@resend.dev}")
    private String fromAddress;

    private final RestClient restClient = RestClient.create();

    @PostConstruct
    void init() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("RESEND_API_KEY is not set — OTP emails will not be sent until it's configured.");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Best-effort send: never throws, so a mail outage can't block visit creation. */
    public void sendVisitOtp(String toAddress, String visitorName, String otpCode, String visitType) {
        send(toAddress, "Your Pavilion entry OTP", """
                Hi %s,

                You've been invited as a %s. A resident will ask you for this code — share it \
                with them so they can confirm the visit:

                %s

                This code expires in 4 hours.
                """.formatted(visitorName, visitType.replace('_', ' '), otpCode));
    }

    /** Best-effort send: never throws, so a mail outage can't block signup. */
    public void sendSignupOtp(String toAddress, String name, String otpCode) {
        send(toAddress, "Verify your Pavilion account", """
                Hi %s,

                Enter this code to finish creating your Pavilion account:

                %s

                This code expires in 10 minutes.
                """.formatted(name, otpCode));
    }

    private void send(String toAddress, String subject, String body) {
        if (!isConfigured()) {
            log.warn("Skipping email to {} — mail is not configured.", toAddress);
            return;
        }
        try {
            Map<String, Object> payload = Map.of(
                    "from", fromAddress,
                    "to", List.of(toAddress),
                    "subject", subject,
                    "text", body);
            restClient.post()
                    .uri(RESEND_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.error("Failed to send email to {}", toAddress, e);
        }
    }
}
