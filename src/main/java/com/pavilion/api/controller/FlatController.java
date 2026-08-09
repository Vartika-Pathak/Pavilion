package com.pavilion.api.controller;

import com.pavilion.api.dto.MastersDtos.FlatChangeRequestRequest;
import com.pavilion.api.dto.MastersDtos.FlatChangeRequestResponse;
import com.pavilion.api.dto.MastersDtos.FlatChangeRequestStatusRequest;
import com.pavilion.api.dto.MastersDtos.FlatDirectoryEntry;
import com.pavilion.api.dto.MastersDtos.FlatRequest;
import com.pavilion.api.dto.MastersDtos.FlatResponse;
import com.pavilion.api.entity.Building;
import com.pavilion.api.entity.Flat;
import com.pavilion.api.entity.FlatChangeRequest;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.BuildingRepository;
import com.pavilion.api.repository.FlatChangeRequestRepository;
import com.pavilion.api.repository.FlatRepository;
import com.pavilion.api.repository.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flats")
@PreAuthorize("hasRole('ADMIN')")
public class FlatController {

    private final FlatRepository flatRepository;
    private final BuildingRepository buildingRepository;
    private final UserRepository userRepository;
    private final FlatChangeRequestRepository flatChangeRequestRepository;

    public FlatController(
            FlatRepository flatRepository, BuildingRepository buildingRepository,
            UserRepository userRepository, FlatChangeRequestRepository flatChangeRequestRepository) {
        this.flatRepository = flatRepository;
        this.buildingRepository = buildingRepository;
        this.userRepository = userRepository;
        this.flatChangeRequestRepository = flatChangeRequestRepository;
    }

    private String buildingNameFor(Long buildingId) {
        return buildingRepository.findById(buildingId)
                .map(Building::getName)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown building"));
    }

    private User residentFor(Long residentId) {
        if (residentId == null) {
            return null;
        }
        User resident = userRepository.findById(residentId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown resident"));
        if (!"resident".equals(resident.getRole())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "That account isn't a resident");
        }
        return resident;
    }

    @GetMapping
    public List<FlatResponse> listFlats() {
        List<Flat> flats = flatRepository.findAll();
        Map<Long, String> buildingNames = buildingNames();
        Map<Long, User> residents = residentsById(flats);
        return flats.stream()
                .map(flat -> FlatResponse.from(flat, buildingNames.get(flat.getBuildingId()), residents.get(flat.getResidentId())))
                .toList();
    }

    @PostMapping
    public ResponseEntity<FlatResponse> createFlat(@Valid @RequestBody FlatRequest body) {
        String buildingName = buildingNameFor(body.buildingId());
        User resident = residentFor(body.residentId());

        Flat flat = new Flat();
        flat.setBuildingId(body.buildingId());
        flat.setFlatNumber(body.flatNumber());
        flat.setFlatType(body.flatType());
        flat.setOccupied(body.occupied());
        flat.setOwnershipType(body.ownershipType());
        flat.setResidentId(body.residentId());
        flat = flatRepository.save(flat);

        return ResponseEntity.status(HttpStatus.CREATED).body(FlatResponse.from(flat, buildingName, resident));
    }

    @PutMapping("/{id}")
    public FlatResponse updateFlat(@PathVariable Long id, @Valid @RequestBody FlatRequest body) {
        Flat flat = flatRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Flat not found"));
        String buildingName = buildingNameFor(body.buildingId());
        User resident = residentFor(body.residentId());

        flat.setBuildingId(body.buildingId());
        flat.setFlatNumber(body.flatNumber());
        flat.setFlatType(body.flatType());
        flat.setOccupied(body.occupied());
        flat.setOwnershipType(body.ownershipType());
        flat.setResidentId(body.residentId());

        return FlatResponse.from(flatRepository.save(flat), buildingName, resident);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlat(@PathVariable Long id) {
        if (!flatRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Flat not found");
        }
        flatRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // Read-only "who lives here" lookup for guards verifying visitors/deliveries at the gate —
    // narrower than the admin Masters view, so it doesn't leak ownership/admin-only detail.
    @GetMapping("/directory")
    @PreAuthorize("hasAnyRole('GUARD', 'ADMIN')")
    public List<FlatDirectoryEntry> directory() {
        List<Flat> flats = flatRepository.findAll();
        Map<Long, String> buildingNames = buildingNames();
        Map<Long, User> residents = residentsById(flats);
        return flats.stream()
                .map(flat -> FlatDirectoryEntry.from(flat, buildingNames.get(flat.getBuildingId()), residents.get(flat.getResidentId())))
                .toList();
    }

    // A resident's own flat, read-only — null residentId match means "not assigned to any flat
    // yet", which is a normal state (not an error) for a freshly signed-up resident.
    @GetMapping("/mine")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FlatResponse> myFlat(@AuthenticationPrincipal User user) {
        return flatRepository.findByResidentId(user.getId())
                .map(flat -> ResponseEntity.ok(FlatResponse.from(flat, buildingNameFor(flat.getBuildingId()), user)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/change-requests")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<FlatChangeRequestResponse> requestChange(
            @PathVariable Long id, @AuthenticationPrincipal User user, @Valid @RequestBody FlatChangeRequestRequest body) {
        Flat flat = flatRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Flat not found"));
        if (!user.getId().equals(flat.getResidentId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "This isn't your flat");
        }

        FlatChangeRequest request = new FlatChangeRequest();
        request.setFlatId(flat.getId());
        request.setResidentId(user.getId());
        request.setResidentName(user.getName());
        request.setMessage(body.message());
        request = flatChangeRequestRepository.save(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(FlatChangeRequestResponse.from(request));
    }

    @GetMapping("/change-requests")
    public List<FlatChangeRequestResponse> listChangeRequests() {
        return flatChangeRequestRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(FlatChangeRequestResponse::from)
                .toList();
    }

    @PatchMapping("/change-requests/{id}/status")
    public FlatChangeRequestResponse updateChangeRequestStatus(
            @PathVariable Long id, @Valid @RequestBody FlatChangeRequestStatusRequest body) {
        FlatChangeRequest request = flatChangeRequestRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Change request not found"));
        request.setStatus(body.status());
        return FlatChangeRequestResponse.from(flatChangeRequestRepository.save(request));
    }

    private Map<Long, String> buildingNames() {
        Map<Long, String> buildingNames = new HashMap<>();
        for (Building building : buildingRepository.findAll()) {
            buildingNames.put(building.getId(), building.getName());
        }
        return buildingNames;
    }

    private Map<Long, User> residentsById(List<Flat> flats) {
        Map<Long, User> residents = new HashMap<>();
        for (Flat flat : flats) {
            if (flat.getResidentId() != null && !residents.containsKey(flat.getResidentId())) {
                userRepository.findById(flat.getResidentId()).ifPresent(u -> residents.put(flat.getResidentId(), u));
            }
        }
        return residents;
    }
}
