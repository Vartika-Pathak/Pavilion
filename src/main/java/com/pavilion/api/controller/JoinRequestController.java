package com.pavilion.api.controller;

import com.pavilion.api.dto.ContentPagesDtos.JoinRequestRequest;
import com.pavilion.api.dto.ContentPagesDtos.JoinRequestResponse;
import com.pavilion.api.entity.JoinRequest;
import com.pavilion.api.repository.JoinRequestRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// A public "request to join the society" form — anyone can submit one, no sign-in required.
@RestController
@RequestMapping("/api/join-requests")
public class JoinRequestController {

    private final JoinRequestRepository joinRequestRepository;

    public JoinRequestController(JoinRequestRepository joinRequestRepository) {
        this.joinRequestRepository = joinRequestRepository;
    }

    @PostMapping
    public ResponseEntity<JoinRequestResponse> submitJoinRequest(@Valid @RequestBody JoinRequestRequest body) {
        JoinRequest request = new JoinRequest();
        request.setName(body.name());
        request.setEmail(body.email());
        request.setFlatNumber(body.flatNumber());
        request.setMessage(body.message());
        request = joinRequestRepository.save(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(JoinRequestResponse.from(request));
    }
}
