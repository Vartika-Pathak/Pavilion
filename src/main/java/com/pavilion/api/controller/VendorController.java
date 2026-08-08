package com.pavilion.api.controller;

import com.pavilion.api.dto.MastersDtos.VendorRequest;
import com.pavilion.api.dto.MastersDtos.VendorResponse;
import com.pavilion.api.entity.Vendor;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.VendorRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendors")
@PreAuthorize("hasRole('ADMIN')")
public class VendorController {

    private final VendorRepository vendorRepository;

    public VendorController(VendorRepository vendorRepository) {
        this.vendorRepository = vendorRepository;
    }

    @GetMapping
    public List<VendorResponse> listVendors() {
        return vendorRepository.findAll().stream().map(VendorResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<VendorResponse> createVendor(@Valid @RequestBody VendorRequest body) {
        Vendor vendor = new Vendor();
        applyRequest(vendor, body);
        vendor = vendorRepository.save(vendor);
        return ResponseEntity.status(HttpStatus.CREATED).body(VendorResponse.from(vendor));
    }

    @PutMapping("/{id}")
    public VendorResponse updateVendor(@PathVariable Long id, @Valid @RequestBody VendorRequest body) {
        Vendor vendor = vendorRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Vendor not found"));
        applyRequest(vendor, body);
        return VendorResponse.from(vendorRepository.save(vendor));
    }

    private void applyRequest(Vendor vendor, VendorRequest body) {
        vendor.setName(body.name());
        vendor.setContactPersonName(body.contactPersonName());
        vendor.setContactNumber(body.contactNumber());
        vendor.setAddress(body.address());
        vendor.setGstNumber(body.gstNumber());
        vendor.setOpeningBalancePaise(body.openingBalancePaise() != null ? body.openingBalancePaise() : 0L);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendor(@PathVariable Long id) {
        if (!vendorRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Vendor not found");
        }
        vendorRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
