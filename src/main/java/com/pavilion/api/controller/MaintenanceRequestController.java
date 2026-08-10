package com.pavilion.api.controller;

import com.pavilion.api.dto.ResidentRequestsDtos.MaintenanceRequestResponse;
import com.pavilion.api.dto.ResidentRequestsDtos.MaintenanceStatusRequest;
import com.pavilion.api.entity.MaintenanceRequest;
import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Vendor;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.MaintenanceRequestRepository;
import com.pavilion.api.repository.VendorRepository;
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
    private final VendorRepository vendorRepository;
    private final FileStorageService fileStorageService;

    public MaintenanceRequestController(
            MaintenanceRequestRepository maintenanceRequestRepository, VendorRepository vendorRepository,
            FileStorageService fileStorageService) {
        this.maintenanceRequestRepository = maintenanceRequestRepository;
        this.vendorRepository = vendorRepository;
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

    // id is a query param rather than a path variable so the Render static-site rewrite rule for
    // this route can be an exact-match proxy (no wildcard, no :splat) — see the /api/uploads
    // endpoint for the same fix and the reasoning behind it.
    @PostMapping("/status")
    @PreAuthorize("hasAnyRole('GUARD', 'ADMIN')")
    public MaintenanceRequestResponse updateStatus(@RequestParam Long id, @Valid @RequestBody MaintenanceStatusRequest body) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));

        // Moving to "in_progress" requires assigning a vendor whose category matches the request's
        // — every other transition leaves whatever vendor is already on the request untouched.
        if ("in_progress".equals(body.status())) {
            if (body.vendorId() == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "A vendor must be assigned to move a request to in_progress");
            }
            Vendor vendor = vendorRepository.findById(body.vendorId())
                    .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "Vendor not found"));
            if (!request.getCategory().equals(vendor.getCategory())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "That vendor isn't tagged for " + request.getCategory() + " work");
            }
            request.setVendorId(vendor.getId());
            request.setVendorName(vendor.getName());
        }

        request.setStatus(body.status());
        request.setUpdatedAt(Instant.now());

        return MaintenanceRequestResponse.from(maintenanceRequestRepository.save(request));
    }

    // Closes the loop from the resident's side, mirroring ComplaintController's confirm/reopen —
    // only the request's own resident can confirm or reject, and only while it's "resolved".
    // id is a query param for the same Render-rewrite reason as updateStatus above.
    @PostMapping("/confirm")
    public MaintenanceRequestResponse confirmResolved(@RequestParam Long id, @AuthenticationPrincipal User user) {
        MaintenanceRequest request = ownedResolvedRequest(id, user);
        request.setStatus("closed");
        request.setUpdatedAt(Instant.now());
        return MaintenanceRequestResponse.from(maintenanceRequestRepository.save(request));
    }

    @PostMapping("/reopen")
    public MaintenanceRequestResponse reopen(@RequestParam Long id, @AuthenticationPrincipal User user) {
        MaintenanceRequest request = ownedResolvedRequest(id, user);
        request.setStatus("open");
        request.setUpdatedAt(Instant.now());
        return MaintenanceRequestResponse.from(maintenanceRequestRepository.save(request));
    }

    private MaintenanceRequest ownedResolvedRequest(Long id, User user) {
        MaintenanceRequest request = maintenanceRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Request not found"));
        if (!user.getId().equals(request.getResidentId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This isn't your request");
        }
        if (!"resolved".equals(request.getStatus())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Only a resolved request can be confirmed or reopened");
        }
        return request;
    }
}
