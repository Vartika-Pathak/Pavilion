package com.pavilion.api.dto;

import com.pavilion.api.entity.BillPayment;
import com.pavilion.api.entity.MaintenanceCollection;
import com.pavilion.api.entity.MaintenanceDiscount;
import com.pavilion.api.entity.MaintenanceRate;
import com.pavilion.api.entity.MaintenanceSettings;
import com.pavilion.api.entity.SpecialContribution;
import com.pavilion.api.entity.VendorBill;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;

public class TransactionsDtos {

    // ---- Maintenance Rates ----

    public record MaintenanceRateResponse(Long id, String flatType, Long monthlyAmountPaise) {
        public static MaintenanceRateResponse from(MaintenanceRate rate) {
            return new MaintenanceRateResponse(rate.getId(), rate.getFlatType(), rate.getMonthlyAmountPaise());
        }
    }

    public record MaintenanceRateRequest(
            @NotNull(message = "Monthly amount is required")
            @Min(value = 0, message = "Monthly amount can't be negative") Long monthlyAmountPaise) {
    }

    // ---- Maintenance Settings ----

    public record MaintenanceSettingsResponse(Long id, Integer dueDay, Integer lateFeePercent, String openingBalanceNote) {
        public static MaintenanceSettingsResponse from(MaintenanceSettings settings) {
            return new MaintenanceSettingsResponse(
                    settings.getId(), settings.getDueDay(), settings.getLateFeePercent(), settings.getOpeningBalanceNote());
        }
    }

    public record UpdateMaintenanceSettingsRequest(
            @NotNull(message = "Due day is required")
            @Min(value = 1, message = "Due day must be between 1 and 28")
            @Max(value = 28, message = "Due day must be between 1 and 28") Integer dueDay,
            @NotNull(message = "Late fee percent is required")
            @Min(value = 0, message = "Late fee percent must be between 0 and 100")
            @Max(value = 100, message = "Late fee percent must be between 0 and 100") Integer lateFeePercent,
            @NotNull(message = "Opening balance note is required") String openingBalanceNote) {
    }

    // ---- Maintenance Discounts ----

    public record MaintenanceDiscountResponse(
            Long id, String name, String discountType, Long value, String description, boolean active) {
        public static MaintenanceDiscountResponse from(MaintenanceDiscount discount) {
            return new MaintenanceDiscountResponse(
                    discount.getId(), discount.getName(), discount.getDiscountType(), discount.getValue(),
                    discount.getDescription(), discount.isActive());
        }
    }

    public record MaintenanceDiscountRequest(
            @NotBlank(message = "Name is required") String name,
            @NotBlank(message = "Discount type is required")
            @Pattern(regexp = "^(percent|fixed)$", message = "discountType must be \"percent\" or \"fixed\"")
            String discountType,
            @NotNull(message = "Value is required")
            @Min(value = 0, message = "Value can't be negative") Long value,
            String description,
            @NotNull(message = "Active is required") Boolean active) {
    }

    // ---- Special Contributions ----

    public record SpecialContributionResponse(
            Long id, String title, String description, Long amountPaise, String dueDate, Instant createdAt) {
        public static SpecialContributionResponse from(SpecialContribution contribution) {
            return new SpecialContributionResponse(
                    contribution.getId(), contribution.getTitle(), contribution.getDescription(),
                    contribution.getAmountPaise(), contribution.getDueDate(), contribution.getCreatedAt());
        }
    }

    public record SpecialContributionRequest(
            @NotBlank(message = "Title is required") String title,
            String description,
            @NotNull(message = "Amount is required")
            @Min(value = 0, message = "Amount can't be negative") Long amountPaise,
            @NotBlank(message = "Due date is required") String dueDate) {
    }

    // ---- Vendor Bills ----

    public record VendorBillResponse(
            Long id, Long vendorId, String vendorName, Long expenseCategoryId, String expenseCategoryName,
            String billNumber, String billDate, Long amountPaise, String description,
            Long paidAmountPaise, String status, Instant createdAt) {

        public static VendorBillResponse from(VendorBill bill, String vendorName, String expenseCategoryName, long paidAmountPaise) {
            String status = statusFor(bill.getAmountPaise(), paidAmountPaise);
            return new VendorBillResponse(
                    bill.getId(), bill.getVendorId(), vendorName, bill.getExpenseCategoryId(), expenseCategoryName,
                    bill.getBillNumber(), bill.getBillDate(), bill.getAmountPaise(), bill.getDescription(),
                    paidAmountPaise, status, bill.getCreatedAt());
        }

        private static String statusFor(long amountPaise, long paidAmountPaise) {
            if (paidAmountPaise <= 0) return "unpaid";
            if (paidAmountPaise >= amountPaise) return "paid";
            return "partially_paid";
        }
    }

    public record VendorBillRequest(
            @NotNull(message = "Vendor is required") Long vendorId,
            @NotNull(message = "Expense category is required") Long expenseCategoryId,
            @NotBlank(message = "Bill number is required") String billNumber,
            @NotBlank(message = "Bill date is required") String billDate,
            @NotNull(message = "Amount is required")
            @Min(value = 1, message = "Amount must be at least 1") Long amountPaise,
            String description) {
    }

    // ---- Bill Payments ----

    public record BillPaymentResponse(
            Long id, Long vendorBillId, Long amountPaise, String paymentDate, String paymentMode,
            String referenceNumber, String notes, Instant createdAt) {
        public static BillPaymentResponse from(BillPayment payment) {
            return new BillPaymentResponse(
                    payment.getId(), payment.getVendorBillId(), payment.getAmountPaise(), payment.getPaymentDate(),
                    payment.getPaymentMode(), payment.getReferenceNumber(), payment.getNotes(), payment.getCreatedAt());
        }
    }

    public record BillPaymentRequest(
            @NotNull(message = "Vendor bill is required") Long vendorBillId,
            @NotNull(message = "Amount is required")
            @Min(value = 1, message = "Amount must be at least 1") Long amountPaise,
            @NotBlank(message = "Payment date is required") String paymentDate,
            @NotBlank(message = "Payment mode is required")
            @Pattern(regexp = "^(cash|cheque|upi|bank_transfer)$", message = "paymentMode must be cash, cheque, upi, or bank_transfer")
            String paymentMode,
            String referenceNumber,
            String notes) {
    }

    // ---- Maintenance Collections ----

    public record MaintenanceCollectionResponse(
            Long id, Long flatId, String buildingName, String flatNumber, String payerName, Long amountPaise,
            String paymentDate, String paymentMode, String forMonth, String referenceNumber, String notes,
            Instant createdAt) {
        public static MaintenanceCollectionResponse from(MaintenanceCollection collection, String buildingName, String flatNumber) {
            return new MaintenanceCollectionResponse(
                    collection.getId(), collection.getFlatId(), buildingName, flatNumber, collection.getPayerName(),
                    collection.getAmountPaise(), collection.getPaymentDate(), collection.getPaymentMode(),
                    collection.getForMonth(), collection.getReferenceNumber(), collection.getNotes(), collection.getCreatedAt());
        }
    }

    public record MaintenanceCollectionRequest(
            @NotNull(message = "Flat is required") Long flatId,
            @NotBlank(message = "Payer name is required") String payerName,
            @NotNull(message = "Amount is required")
            @Min(value = 1, message = "Amount must be at least 1") Long amountPaise,
            @NotBlank(message = "Payment date is required") String paymentDate,
            @NotBlank(message = "Payment mode is required")
            @Pattern(regexp = "^(cash|cheque|upi|bank_transfer)$", message = "paymentMode must be cash, cheque, upi, or bank_transfer")
            String paymentMode,
            @NotBlank(message = "For month is required") String forMonth,
            String referenceNumber,
            String notes) {
    }

    public record BackfillMaintenanceCollectionsRequest(
            @Min(value = 1, message = "months must be at least 1")
            @Max(value = 12, message = "months must be at most 12") Integer months) {
    }

    public record BackfillMaintenanceCollectionsResponse(
            java.util.List<String> monthsBackfilled, int createdCount, int skippedCount) {
    }
}
