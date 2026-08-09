package com.pavilion.api.controller;

import com.pavilion.api.dto.TransactionsDtos.SpecialContributionRequest;
import com.pavilion.api.dto.TransactionsDtos.SpecialContributionResponse;
import com.pavilion.api.entity.SpecialContribution;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.SpecialContributionRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/special-contributions")
@PreAuthorize("hasRole('ADMIN')")
public class SpecialContributionController {

    private final SpecialContributionRepository specialContributionRepository;

    public SpecialContributionController(SpecialContributionRepository specialContributionRepository) {
        this.specialContributionRepository = specialContributionRepository;
    }

    @GetMapping
    public List<SpecialContributionResponse> listSpecialContributions() {
        return specialContributionRepository.findAll().stream().map(SpecialContributionResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<SpecialContributionResponse> createSpecialContribution(@Valid @RequestBody SpecialContributionRequest body) {
        SpecialContribution contribution = new SpecialContribution();
        contribution.setTitle(body.title());
        contribution.setDescription(body.description());
        contribution.setAmountPaise(body.amountPaise());
        contribution.setDueDate(body.dueDate());
        contribution = specialContributionRepository.save(contribution);
        return ResponseEntity.status(HttpStatus.CREATED).body(SpecialContributionResponse.from(contribution));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSpecialContribution(@PathVariable Long id) {
        if (!specialContributionRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Special contribution not found");
        }
        specialContributionRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
