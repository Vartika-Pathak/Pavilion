package com.pavilion.api.controller;

import com.pavilion.api.dto.VehicleDtos.VehicleRequest;
import com.pavilion.api.dto.VehicleDtos.VehicleResponse;
import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Vehicle;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.ParkingPassRepository;
import com.pavilion.api.repository.VehicleRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleRepository vehicleRepository;
    private final ParkingPassRepository parkingPassRepository;

    public VehicleController(VehicleRepository vehicleRepository, ParkingPassRepository parkingPassRepository) {
        this.vehicleRepository = vehicleRepository;
        this.parkingPassRepository = parkingPassRepository;
    }

    // Guards and admins see every registered vehicle (for gate lookups); residents only see
    // their own.
    @GetMapping
    public List<VehicleResponse> listVehicles(@AuthenticationPrincipal User user) {
        boolean canSeeAll = "guard".equals(user.getRole()) || "admin".equals(user.getRole());
        List<Vehicle> vehicles = canSeeAll
                ? vehicleRepository.findAllByOrderByCreatedAtDesc()
                : vehicleRepository.findByResidentIdOrderByCreatedAtDesc(user.getId());
        return vehicles.stream().map(VehicleResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<VehicleResponse> registerVehicle(
            @AuthenticationPrincipal User user, @Valid @RequestBody VehicleRequest body) {
        if (parkingPassRepository.findByFlatNumber(user.getFlatNumber()).isEmpty()) {
            throw new ApiException(HttpStatus.PAYMENT_REQUIRED, "Buy a parking pass for your flat before registering a vehicle");
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setResidentId(user.getId());
        vehicle.setOwnerName(user.getName());
        vehicle.setFlatNumber(user.getFlatNumber());
        vehicle.setPlateNumber(body.plateNumber());
        vehicle.setVehicleType(body.vehicleType());
        vehicle.setOwnerPhone(body.ownerPhone());
        vehicle = vehicleRepository.save(vehicle);
        return ResponseEntity.status(HttpStatus.CREATED).body(VehicleResponse.from(vehicle));
    }

    // id is a query param rather than a path variable — same reasoning as
    // /api/maintenance/status: it keeps the Render static-site rewrite rule for this route an
    // exact-match proxy instead of depending on wildcard :splat substitution.
    @DeleteMapping
    public ResponseEntity<Void> deleteVehicle(@AuthenticationPrincipal User user, @RequestParam Long id) {
        Vehicle vehicle = vehicleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vehicle not found"));
        boolean isOwner = user.getId().equals(vehicle.getResidentId());
        boolean isAdmin = "admin".equals(user.getRole());
        if (!isOwner && !isAdmin) {
            throw new ApiException(HttpStatus.FORBIDDEN, "You can only remove your own vehicle");
        }
        vehicleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
