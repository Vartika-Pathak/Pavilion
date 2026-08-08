package com.pavilion.api.controller;

import com.pavilion.api.dto.MastersDtos.BuildingRequest;
import com.pavilion.api.dto.MastersDtos.BuildingResponse;
import com.pavilion.api.entity.Building;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.BuildingRepository;
import com.pavilion.api.repository.FlatRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/buildings")
@PreAuthorize("hasRole('ADMIN')")
public class BuildingController {

    private final BuildingRepository buildingRepository;
    private final FlatRepository flatRepository;

    public BuildingController(BuildingRepository buildingRepository, FlatRepository flatRepository) {
        this.buildingRepository = buildingRepository;
        this.flatRepository = flatRepository;
    }

    @GetMapping
    public List<BuildingResponse> listBuildings() {
        return buildingRepository.findAll().stream().map(BuildingResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<BuildingResponse> createBuilding(@Valid @RequestBody BuildingRequest body) {
        Building building = new Building();
        building.setName(body.name());
        building.setTotalFlats(body.totalFlats());
        building = buildingRepository.save(building);
        return ResponseEntity.status(HttpStatus.CREATED).body(BuildingResponse.from(building));
    }

    @PutMapping("/{id}")
    public BuildingResponse updateBuilding(@PathVariable Long id, @Valid @RequestBody BuildingRequest body) {
        Building building = buildingRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Building not found"));
        building.setName(body.name());
        building.setTotalFlats(body.totalFlats());
        return BuildingResponse.from(buildingRepository.save(building));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBuilding(@PathVariable Long id) {
        if (!buildingRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Building not found");
        }
        if (flatRepository.existsByBuildingId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "This building still has flats assigned to it — remove those first");
        }
        buildingRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
