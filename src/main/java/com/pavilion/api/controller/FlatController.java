package com.pavilion.api.controller;

import com.pavilion.api.dto.MastersDtos.FlatChangeRequestRequest;
import com.pavilion.api.dto.MastersDtos.FlatChangeRequestResponse;
import com.pavilion.api.dto.MastersDtos.FlatChangeRequestStatusRequest;
import com.pavilion.api.dto.MastersDtos.FlatDirectoryEntry;
import com.pavilion.api.dto.MastersDtos.FlatRequest;
import com.pavilion.api.dto.MastersDtos.FlatResponse;
import com.pavilion.api.dto.MastersDtos.SyncFlatResidentsResult;
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    // Letter-hyphen-digits, e.g. "A-101" — same shape AdminController requires at signup for a
    // resident's flatNumber. The letter is treated as the building code when creating a flat.
    private static final Pattern FLAT_NUMBER_PATTERN = Pattern.compile("^([A-Za-z])-([0-9]{1,3})$");

    // For accounts that existed before Flat Resident shipped — matches each resident's profile
    // flatNumber against an existing Flat with the same number and links it. If none exists yet,
    // creates one (and its Building, from the flatNumber's letter prefix) rather than leaving the
    // resident unassigned, since flatType/ownershipType can't be inferred from a bare number and
    // are set to placeholder defaults (1bhk, occupied, owner) flagged for the admin to review.
    // Repeatable: already-linked flats are left untouched, so running this again after fixing an
    // issue only picks up what's still outstanding.
    @PostMapping("/sync-residents")
    public SyncFlatResidentsResult syncResidents() {
        List<Flat> flats = flatRepository.findAll();
        Map<String, List<Flat>> flatsByNumber = new HashMap<>();
        for (Flat flat : flats) {
            flatsByNumber.computeIfAbsent(normalizeFlatNumber(flat.getFlatNumber()), k -> new ArrayList<>()).add(flat);
        }

        int matched = 0;
        int created = 0;
        List<String> issues = new ArrayList<>();

        for (User resident : userRepository.findAll()) {
            if (!"resident".equals(resident.getRole())) {
                continue;
            }
            String key = normalizeFlatNumber(resident.getFlatNumber());
            List<Flat> candidates = flatsByNumber.getOrDefault(key, List.of());

            if (candidates.size() > 1) {
                issues.add(resident.getName() + " (" + resident.getEmail() + "): flat number \""
                        + resident.getFlatNumber() + "\" matches more than one flat — assign manually");
                continue;
            }

            if (candidates.size() == 1) {
                Flat flat = candidates.get(0);
                if (flat.getResidentId() == null) {
                    flat.setResidentId(resident.getId());
                    flatRepository.save(flat);
                    matched++;
                } else if (!flat.getResidentId().equals(resident.getId())) {
                    issues.add(resident.getName() + " (" + resident.getEmail() + "): flat \"" + resident.getFlatNumber()
                            + "\" is already assigned to someone else — check for a mismatch");
                }
                continue;
            }

            Matcher m = FLAT_NUMBER_PATTERN.matcher(
                    resident.getFlatNumber() == null ? "" : resident.getFlatNumber().trim());
            if (!m.matches()) {
                issues.add(resident.getName() + " (" + resident.getEmail() + "): flat number \""
                        + resident.getFlatNumber() + "\" doesn't look like letter-hyphen-digits (e.g. A-101) — create it manually");
                continue;
            }

            String buildingCode = m.group(1).toUpperCase();
            Building building = buildingRepository.findByNameIgnoreCase(buildingCode).orElseGet(() -> {
                Building newBuilding = new Building();
                newBuilding.setName(buildingCode);
                newBuilding.setTotalFlats(1);
                return buildingRepository.save(newBuilding);
            });

            Flat flat = new Flat();
            flat.setBuildingId(building.getId());
            flat.setFlatNumber(resident.getFlatNumber().trim());
            flat.setFlatType("1bhk");
            flat.setOccupied(true);
            flat.setOwnershipType("owner");
            flat.setResidentId(resident.getId());
            flat = flatRepository.save(flat);
            flatsByNumber.computeIfAbsent(key, k -> new ArrayList<>()).add(flat);
            created++;
            issues.add(resident.getName() + " (" + resident.getEmail() + "): created flat \"" + resident.getFlatNumber()
                    + "\" in building \"" + buildingCode + "\" with placeholder type/ownership — review it in Flat Resident");
        }

        return new SyncFlatResidentsResult(matched, created, issues);
    }

    private static String normalizeFlatNumber(String flatNumber) {
        return flatNumber == null ? "" : flatNumber.trim().toUpperCase();
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
