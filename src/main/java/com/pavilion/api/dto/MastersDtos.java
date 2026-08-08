package com.pavilion.api.dto;

import com.pavilion.api.entity.Building;
import com.pavilion.api.entity.ExpenseCategory;
import com.pavilion.api.entity.Flat;
import com.pavilion.api.entity.Society;
import com.pavilion.api.entity.Vendor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class MastersDtos {

    public record SocietyInfoResponse(Long id, String name, String address, String contactNumber, String email) {
        public static SocietyInfoResponse from(Society society) {
            return new SocietyInfoResponse(
                    society.getId(), society.getName(), society.getAddress(),
                    society.getContactNumber(), society.getEmail());
        }
    }

    public record UpdateSocietyInfoRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Address is required") String address,
            @NotBlank(message = "Contact number is required") String contactNumber,
            @NotBlank(message = "Email is required") String email) {
    }

    public record BuildingResponse(Long id, String name, Integer totalFlats) {
        public static BuildingResponse from(Building building) {
            return new BuildingResponse(building.getId(), building.getName(), building.getTotalFlats());
        }
    }

    public record BuildingRequest(
            @NotBlank(message = "Name is required") String name,
            @NotNull(message = "Total flats is required") @Min(value = 1, message = "Total flats must be at least 1") Integer totalFlats) {
    }

    public record FlatResponse(
            Long id, Long buildingId, String buildingName, String flatNumber,
            String flatType, boolean occupied, String ownershipType) {
        public static FlatResponse from(Flat flat, String buildingName) {
            return new FlatResponse(
                    flat.getId(), flat.getBuildingId(), buildingName, flat.getFlatNumber(),
                    flat.getFlatType(), flat.isOccupied(), flat.getOwnershipType());
        }
    }

    public record FlatRequest(
            @NotNull(message = "Building is required") Long buildingId,
            @NotBlank(message = "Flat number is required") String flatNumber,
            @NotBlank(message = "Flat type is required")
            @Pattern(regexp = "^(1bhk|2bhk|3bhk|4bhk)$", message = "flatType must be one of 1bhk, 2bhk, 3bhk, 4bhk")
            String flatType,
            @NotNull(message = "Occupied is required") Boolean occupied,
            @NotBlank(message = "Ownership type is required")
            @Pattern(regexp = "^(owner|rented)$", message = "ownershipType must be \"owner\" or \"rented\"")
            String ownershipType) {
    }

    public record ExpenseCategoryResponse(Long id, String name, Integer gstSlabPercent) {
        public static ExpenseCategoryResponse from(ExpenseCategory category) {
            return new ExpenseCategoryResponse(category.getId(), category.getName(), category.getGstSlabPercent());
        }
    }

    public record ExpenseCategoryRequest(
            @NotBlank(message = "Name is required") String name,
            @NotNull(message = "GST slab is required")
            @Min(value = 0, message = "GST slab must be between 0 and 100")
            @Max(value = 100, message = "GST slab must be between 0 and 100")
            Integer gstSlabPercent) {
    }

    public record VendorResponse(
            Long id, String name, String contactPersonName, String contactNumber,
            String address, String gstNumber, Long openingBalancePaise) {
        public static VendorResponse from(Vendor vendor) {
            return new VendorResponse(
                    vendor.getId(), vendor.getName(), vendor.getContactPersonName(), vendor.getContactNumber(),
                    vendor.getAddress(), vendor.getGstNumber(), vendor.getOpeningBalancePaise());
        }
    }

    public record VendorRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Contact person name is required") String contactPersonName,
            @NotBlank(message = "Contact number is required") String contactNumber,
            String address,
            String gstNumber,
            @Min(value = 0, message = "Opening balance can't be negative") Long openingBalancePaise) {
    }
}
