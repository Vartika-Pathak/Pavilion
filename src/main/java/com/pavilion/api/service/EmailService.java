package com.pavilion.api.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromAddress;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @PostConstruct
    void init() {
        if (fromAddress == null || fromAddress.isBlank()) {
            log.warn("MAIL_USERNAME is not set — visitor OTP emails will not be sent until it's configured.");
        }
    }

    public boolean isConfigured() {
        return fromAddress != null && !fromAddress.isBlank();
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
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toAddress);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send email to {}", toAddress, e);
        }
    }
}
