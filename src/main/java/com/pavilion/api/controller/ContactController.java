package com.pavilion.api.controller;

import com.pavilion.api.dto.ContentPagesDtos.ContactMessageRequest;
import com.pavilion.api.dto.ContentPagesDtos.ContactMessageResponse;
import com.pavilion.api.entity.ContactMessage;
import com.pavilion.api.repository.ContactMessageRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// A public contact form — anyone can submit one, no sign-in required.
@RestController
@RequestMapping("/api/contact")
public class ContactController {

    private final ContactMessageRepository contactMessageRepository;

    public ContactController(ContactMessageRepository contactMessageRepository) {
        this.contactMessageRepository = contactMessageRepository;
    }

    @PostMapping
    public ResponseEntity<ContactMessageResponse> sendContactMessage(@Valid @RequestBody ContactMessageRequest body) {
        ContactMessage message = new ContactMessage();
        message.setName(body.name());
        message.setEmail(body.email());
        message.setSubject(body.subject());
        message.setMessage(body.message());
        message = contactMessageRepository.save(message);
        return ResponseEntity.status(HttpStatus.CREATED).body(ContactMessageResponse.from(message));
    }
}
