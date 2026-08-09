package com.pavilion.api.controller;

import com.pavilion.api.dto.ResidentRequestsDtos.ComplaintReopenRequest;
import com.pavilion.api.dto.ResidentRequestsDtos.ComplaintRequest;
import com.pavilion.api.dto.ResidentRequestsDtos.ComplaintResponse;
import com.pavilion.api.dto.ResidentRequestsDtos.ComplaintStatusRequest;
import com.pavilion.api.entity.Complaint;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.ComplaintRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/complaints")
public class ComplaintController {

    private final ComplaintRepository complaintRepository;

    public ComplaintController(ComplaintRepository complaintRepository) {
        this.complaintRepository = complaintRepository;
    }

    // Guards and admins see every complaint; residents only see their own.
    @GetMapping
    public List<ComplaintResponse> listComplaints(@AuthenticationPrincipal User user) {
        boolean canSeeAll = "guard".equals(user.getRole()) || "admin".equals(user.getRole());
        List<Complaint> complaints = canSeeAll
                ? complaintRepository.findAllByOrderByCreatedAtDesc()
                : complaintRepository.findByResidentIdOrderByCreatedAtDesc(user.getId());
        return complaints.stream().map(ComplaintResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<ComplaintResponse> createComplaint(
            @AuthenticationPrincipal User user, @Valid @RequestBody ComplaintRequest body) {
        Complaint complaint = new Complaint();
        complaint.setResidentId(user.getId());
        complaint.setResidentName(user.getName());
        complaint.setResidentFlatNumber(user.getFlatNumber());
        complaint.setCategory(body.category());
        complaint.setDescription(body.description());
        complaint = complaintRepository.save(complaint);
        return ResponseEntity.status(HttpStatus.CREATED).body(ComplaintResponse.from(complaint));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('GUARD', 'ADMIN')")
    public ComplaintResponse updateStatus(@PathVariable Long id, @Valid @RequestBody ComplaintStatusRequest body) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Complaint not found"));

        complaint.setStatus(body.status());
        if (body.resolutionNote() != null) {
            complaint.setResolutionNote(body.resolutionNote());
        }
        complaint.setUpdatedAt(Instant.now());

        return ComplaintResponse.from(complaintRepository.save(complaint));
    }

    // Closes the loop from the resident's side: only the complaint's own resident can confirm or
    // reject a resolution, and only while it's actually sitting in "resolved" — admin/guard still
    // own every other transition via updateStatus above.
    @PostMapping("/{id}/confirm")
    public ComplaintResponse confirmResolved(@PathVariable Long id, @AuthenticationPrincipal User user) {
        Complaint complaint = ownedResolvedComplaint(id, user);
        complaint.setStatus("closed");
        complaint.setUpdatedAt(Instant.now());
        return ComplaintResponse.from(complaintRepository.save(complaint));
    }

    @PostMapping("/{id}/reopen")
    public ComplaintResponse reopen(
            @PathVariable Long id, @AuthenticationPrincipal User user,
            @RequestBody(required = false) ComplaintReopenRequest body) {
        Complaint complaint = ownedResolvedComplaint(id, user);
        complaint.setStatus("open");
        if (body != null && body.note() != null && !body.note().isBlank()) {
            String existing = complaint.getResolutionNote();
            complaint.setResolutionNote(
                    (existing != null && !existing.isBlank() ? existing + "\n\n" : "") + "Resident wasn't satisfied: " + body.note());
        }
        complaint.setUpdatedAt(Instant.now());
        return ComplaintResponse.from(complaintRepository.save(complaint));
    }

    private Complaint ownedResolvedComplaint(Long id, User user) {
        Complaint complaint = complaintRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Complaint not found"));
        if (!user.getId().equals(complaint.getResidentId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This isn't your complaint");
        }
        if (!"resolved".equals(complaint.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only a resolved complaint can be confirmed or reopened");
        }
        return complaint;
    }
}
