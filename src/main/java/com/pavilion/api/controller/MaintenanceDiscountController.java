package com.pavilion.api.controller;

import com.pavilion.api.dto.TransactionsDtos.MaintenanceDiscountRequest;
import com.pavilion.api.dto.TransactionsDtos.MaintenanceDiscountResponse;
import com.pavilion.api.entity.MaintenanceDiscount;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.MaintenanceDiscountRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/maintenance-discounts")
@PreAuthorize("hasRole('ADMIN')")
public class MaintenanceDiscountController {

    private final MaintenanceDiscountRepository maintenanceDiscountRepository;

    public MaintenanceDiscountController(MaintenanceDiscountRepository maintenanceDiscountRepository) {
        this.maintenanceDiscountRepository = maintenanceDiscountRepository;
    }

    @GetMapping
    public List<MaintenanceDiscountResponse> listMaintenanceDiscounts() {
        return maintenanceDiscountRepository.findAll().stream().map(MaintenanceDiscountResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<MaintenanceDiscountResponse> createMaintenanceDiscount(@Valid @RequestBody MaintenanceDiscountRequest body) {
        MaintenanceDiscount discount = new MaintenanceDiscount();
        applyRequest(discount, body);
        discount = maintenanceDiscountRepository.save(discount);
        return ResponseEntity.status(HttpStatus.CREATED).body(MaintenanceDiscountResponse.from(discount));
    }

    @PutMapping("/{id}")
    public MaintenanceDiscountResponse updateMaintenanceDiscount(
            @PathVariable Long id, @Valid @RequestBody MaintenanceDiscountRequest body) {
        MaintenanceDiscount discount = maintenanceDiscountRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Maintenance discount not found"));
        applyRequest(discount, body);
        return MaintenanceDiscountResponse.from(maintenanceDiscountRepository.save(discount));
    }

    private void applyRequest(MaintenanceDiscount discount, MaintenanceDiscountRequest body) {
        discount.setName(body.name());
        discount.setDiscountType(body.discountType());
        discount.setValue(body.value());
        discount.setDescription(body.description());
        discount.setActive(body.active());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMaintenanceDiscount(@PathVariable Long id) {
        if (!maintenanceDiscountRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Maintenance discount not found");
        }
        maintenanceDiscountRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
