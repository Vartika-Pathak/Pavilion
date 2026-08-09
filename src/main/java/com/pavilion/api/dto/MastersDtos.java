package com.pavilion.api.dto;

import com.pavilion.api.entity.Building;
import com.pavilion.api.entity.ExpenseCategory;
import com.pavilion.api.entity.Flat;
import com.pavilion.api.entity.FlatChangeRequest;
import com.pavilion.api.entity.Society;
import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Vendor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.time.Instant;
import java.util.List;

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
            String flatType, boolean occupied, String ownershipType,
            Long residentId, String residentName, String residentEmail) {
        // resident is null when the flat has no assigned resident yet.
        public static FlatResponse from(Flat flat, String buildingName, User resident) {
            return new FlatResponse(
                    flat.getId(), flat.getBuildingId(), buildingName, flat.getFlatNumber(),
                    flat.getFlatType(), flat.isOccupied(), flat.getOwnershipType(),
                    flat.getResidentId(), resident != null ? resident.getName() : null,
                    resident != null ? resident.getEmail() : null);
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
            String ownershipType,
            // Null/omitted means "no resident assigned yet" — not every flat is occupied by a
            // signed-up account (could be vacant, or occupied by someone who hasn't signed up).
            Long residentId) {
    }

    // The guard-facing read-only lookup — deliberately narrower than FlatResponse (no ownership
    // type, no ids beyond what's needed to identify the flat/resident), since guards need "who
    // lives here" for verifying visitors, not the full Masters record.
    public record FlatDirectoryEntry(
            Long flatId, String buildingName, String flatNumber, boolean occupied,
            String residentName, String residentEmail) {
        public static FlatDirectoryEntry from(Flat flat, String buildingName, User resident) {
            return new FlatDirectoryEntry(
                    flat.getId(), buildingName, flat.getFlatNumber(), flat.isOccupied(),
                    resident != null ? resident.getName() : null,
                    resident != null ? resident.getEmail() : null);
        }
    }

    public record FlatChangeRequestRequest(@NotBlank(message = "Message is required") String message) {
    }

    public record FlatChangeRequestResponse(
            Long id, Long flatId, Long residentId, String residentName, String message, String status,
            Instant createdAt) {
        public static FlatChangeRequestResponse from(FlatChangeRequest request) {
            return new FlatChangeRequestResponse(
                    request.getId(), request.getFlatId(), request.getResidentId(), request.getResidentName(),
                    request.getMessage(), request.getStatus(), request.getCreatedAt());
        }
    }

    public record FlatChangeRequestStatusRequest(
            @NotBlank(message = "Status is required")
            @Pattern(regexp = "^(pending|reviewed)$", message = "status must be pending or reviewed")
            String status) {
    }

    // A one-time (but safely re-runnable) catch-up for accounts that existed before flats could
    // be assigned to a resident. For each resident, matches their free-text profile flatNumber
    // against an existing Flat first; only if none exists does it create one (and its Building,
    // from the flatNumber's letter prefix — "A-101" -> building "A"), since flatType/ownershipType
    // can't be inferred from a bare number and are set to placeholder defaults the admin should
    // review. Never overwrites an assignment that's already set. Ambiguous/unparseable cases are
    // surfaced in issues for the admin to resolve by hand in Flat Resident.
    public record SyncFlatResidentsResult(int matchedCount, int createdCount, List<String> issues) {
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
