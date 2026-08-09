package com.pavilion.api.controller;

import com.pavilion.api.dto.ResidentRequestsDtos.MaintenanceRequestResponse;
import com.pavilion.api.dto.ResidentRequestsDtos.MaintenanceStatusRequest;
import com.pavilion.api.entity.MaintenanceRequest;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.MaintenanceRequestRepository;
import com.pavilion.api.service.FileStorageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/maintenance")
public class MaintenanceRequestController {

    private static final Set<String> CATEGORIES = Set.of("plumbing", "electrical", "appliance", "structural", "other");

    private final MaintenanceRequestRepository maintenanceRequestRepository;
    private final FileStorageService fileStorageService;

    public MaintenanceRequestController(
            MaintenanceRequestRepository maintenanceRequestRepository, FileStorageService fileStorageService) {
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.fileStorageService = fileStorageService;
    }

    // Guards and admins see every request; residents only see their own.
    @GetMapping
    public List<MaintenanceRequestResponse> listRequests(@AuthenticationPrincipal User user) {
        boolean canSeeAll = "guard".equals(user.getRole()) || "admin".equals(user.getRole());
        List<MaintenanceRequest> requests = canSeeAll
                ? maintenanceRequestRepository.findAllByOrderByCreatedAtDesc()
                : maintenanceRequestRepository.findByResidentIdOrderByCreatedAtDesc(user.getId());
        return requests.stream().map(MaintenanceRequestResponse::from).toList();
    }

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<MaintenanceRequestResponse> createRequest(
            @AuthenticationPrincipal User user,
            @RequestParam String category,
            @RequestParam String description,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos) {
        if (!CATEGORIES.contains(category)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "category must be one of " + CATEGORIES);
        }
        if (description == null || description.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Description is required");
        }

        List<String> photoUrls = List.of();
        try {
            photoUrls = fileStorageService.storeImages(photos);
        } catch (ApiException e) {
            fileStorageService.deleteByUrls(photoUrls);
            throw e;
        }

        MaintenanceRequest request = new MaintenanceRequest();
        request.setResidentId(user.getId());
        request.setResidentName(user.getName());
        request.setResidentFlatNumber(user.getFlatNumber());
        request.setCategory(category);
        request.setDescription(description);
        request.setPhotoUrls(photoUrls);
        request = maintenanceRequestRepository.save(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(MaintenanceRequestResponse.from(request));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('GUARD', 'ADMIN')")
    public MaintenanceRequestResponse updateStatus(@PathVariable Long id, @Valid @RequestBody MaintenanceStatusRequest body) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));

        request.setStatus(body.status());
        request.setUpdatedAt(Instant.now());

        return MaintenanceRequestResponse.from(maintenanceRequestRepository.save(request));
    }
}
