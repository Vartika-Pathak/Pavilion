package com.pavilion.api.dto;

import com.pavilion.api.entity.Vehicle;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public class VehicleDtos {

    public record VehicleResponse(
            Long id, String plateNumber, String vehicleType, String ownerName, String flatNumber,
            String ownerPhone, Instant createdAt) {
        public static VehicleResponse from(Vehicle vehicle) {
            return new VehicleResponse(
                    vehicle.getId(), vehicle.getPlateNumber(), vehicle.getVehicleType(), vehicle.getOwnerName(),
                    vehicle.getFlatNumber(), vehicle.getOwnerPhone(), vehicle.getCreatedAt());
        }
    }

    public record VehicleRequest(
            @NotBlank(message = "Plate number is required") String plateNumber,
            @NotBlank(message = "Vehicle type is required")
            @Pattern(regexp = "^(car|bike|other)$", message = "vehicleType must be car, bike, or other")
            String vehicleType,
            @NotBlank(message = "Owner phone is required") String ownerPhone) {
    }
}
