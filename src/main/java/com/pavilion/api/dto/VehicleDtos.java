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
            @NotBlank(message = "Plate number is required")
            @Pattern(
                    regexp = "^([A-Z]{2}[0-9]{2}[A-Z]{2}[0-9]{4}|[0-9]{2}BH[0-9]{4}[A-Z]{1,2})$",
                    message = "Plate number must look like AA00AA0000, or 00BH0000A for a Bharat-series plate — "
                            + "letters and digits only")
            String plateNumber,
            @NotBlank(message = "Vehicle type is required")
            @Pattern(regexp = "^(car|bike|other)$", message = "vehicleType must be car, bike, or other")
            String vehicleType,
            @NotBlank(message = "Owner phone is required") String ownerPhone) {

        // Accepts how people naturally type a plate (lowercase, spaces, hyphens) and normalizes it
        // before the @Pattern check above runs, so "mh 12 ab-1234" still validates and stores as
        // "MH12AB1234".
        public VehicleRequest {
            if (plateNumber != null) {
                plateNumber = plateNumber.trim().toUpperCase().replaceAll("[\\s-]", "");
            }
        }
    }
}
