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

// Sends over MailerSend's HTTPS API rather than raw SMTP — many free hosting
// tiers (Render included) block outbound SMTP ports to prevent spam abuse,
// but plain HTTPS always goes through. On MailerSend's trial tier, sending
// is restricted to a small set of verified recipients until a domain is
// verified — see the README for details.
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final String MAILERSEND_URL = "https://api.mailersend.com/v1/email";

    @Value("${mailersend.api-key:}")
    private String apiKey;

    @Value("${mail.from:}")
    private String fromAddress;

    private final RestClient restClient = RestClient.create();

    @PostConstruct
    void init() {
        if (!isConfigured()) {
            log.warn("MAILERSEND_API_KEY / MAIL_FROM are not set — OTP emails will not be sent until configured.");
        }
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank() && fromAddress != null && !fromAddress.isBlank();
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
                    "from", Map.of("email", fromAddress),
                    "to", List.of(Map.of("email", toAddress)),
                    "subject", subject,
                    "text", body);
            restClient.post()
                    .uri(MAILERSEND_URL)
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
