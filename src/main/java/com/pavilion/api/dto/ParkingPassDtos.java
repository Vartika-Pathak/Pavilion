package com.pavilion.api.dto;

import com.pavilion.api.entity.ParkingPass;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;

public class ParkingPassDtos {

    public record ParkingPassResponse(
            Long id, String flatNumber, String purchasedByName, Integer amountPaidCents, Instant createdAt) {
        public static ParkingPassResponse from(ParkingPass pass) {
            return new ParkingPassResponse(
                    pass.getId(), pass.getFlatNumber(), pass.getPurchasedByName(), pass.getAmountPaidCents(),
                    pass.getCreatedAt());
        }
    }

    public record PurchaseParkingPassResult(String status, ParkingPassResponse pass, String checkoutUrl) {
    }

    public record ConfirmParkingPassRequest(@NotBlank(message = "Session id is required") String sessionId) {
    }
}
