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
        if (!isConfigured()) {
            log.warn("Skipping OTP email to {} — mail is not configured.", toAddress);
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toAddress);
            message.setSubject("Your Pavilion entry OTP");
            message.setText("""
                    Hi %s,

                    You've been invited as a %s. Share this OTP with the security guard at the gate to enter:

                    %s

                    This code expires in 4 hours.
                    """.formatted(visitorName, visitType.replace('_', ' '), otpCode));
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}", toAddress, e);
        }
    }
}
