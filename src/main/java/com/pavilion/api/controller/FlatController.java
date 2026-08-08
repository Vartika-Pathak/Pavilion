package com.pavilion.api.controller;

import com.pavilion.api.dto.MastersDtos.FlatRequest;
import com.pavilion.api.dto.MastersDtos.FlatResponse;
import com.pavilion.api.entity.Building;
import com.pavilion.api.entity.Flat;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.BuildingRepository;
import com.pavilion.api.repository.FlatRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

    public FlatController(FlatRepository flatRepository, BuildingRepository buildingRepository) {
        this.flatRepository = flatRepository;
        this.buildingRepository = buildingRepository;
    }

    private String buildingNameFor(Long buildingId) {
        return buildingRepository.findById(buildingId)
                .map(Building::getName)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown building"));
    }

    @GetMapping
    public List<FlatResponse> listFlats() {
        List<Flat> flats = flatRepository.findAll();
        Map<Long, String> buildingNames = new HashMap<>();
        for (Building building : buildingRepository.findAll()) {
            buildingNames.put(building.getId(), building.getName());
        }
        return flats.stream()
                .map(flat -> FlatResponse.from(flat, buildingNames.get(flat.getBuildingId())))
                .toList();
    }

    @PostMapping
    public ResponseEntity<FlatResponse> createFlat(@Valid @RequestBody FlatRequest body) {
        String buildingName = buildingNameFor(body.buildingId());

        Flat flat = new Flat();
        flat.setBuildingId(body.buildingId());
        flat.setFlatNumber(body.flatNumber());
        flat.setFlatType(body.flatType());
        flat.setOccupied(body.occupied());
        flat.setOwnershipType(body.ownershipType());
        flat = flatRepository.save(flat);

        return ResponseEntity.status(HttpStatus.CREATED).body(FlatResponse.from(flat, buildingName));
    }

    @PutMapping("/{id}")
    public FlatResponse updateFlat(@PathVariable Long id, @Valid @RequestBody FlatRequest body) {
        Flat flat = flatRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Flat not found"));
        String buildingName = buildingNameFor(body.buildingId());

        flat.setBuildingId(body.buildingId());
        flat.setFlatNumber(body.flatNumber());
        flat.setFlatType(body.flatType());
        flat.setOccupied(body.occupied());
        flat.setOwnershipType(body.ownershipType());

        return FlatResponse.from(flatRepository.save(flat), buildingName);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFlat(@PathVariable Long id) {
        if (!flatRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Flat not found");
        }
        flatRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
