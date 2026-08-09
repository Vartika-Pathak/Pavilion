package com.pavilion.api.controller;

import com.pavilion.api.dto.TransactionsDtos.VendorBillRequest;
import com.pavilion.api.dto.TransactionsDtos.VendorBillResponse;
import com.pavilion.api.entity.ExpenseCategory;
import com.pavilion.api.entity.Vendor;
import com.pavilion.api.entity.VendorBill;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.BillPaymentRepository;
import com.pavilion.api.repository.ExpenseCategoryRepository;
import com.pavilion.api.repository.VendorBillRepository;
import com.pavilion.api.repository.VendorRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vendor-bills")
@PreAuthorize("hasRole('ADMIN')")
public class VendorBillController {

    private final VendorBillRepository vendorBillRepository;
    private final VendorRepository vendorRepository;
    private final ExpenseCategoryRepository expenseCategoryRepository;
    private final BillPaymentRepository billPaymentRepository;

    public VendorBillController(
            VendorBillRepository vendorBillRepository,
            VendorRepository vendorRepository,
            ExpenseCategoryRepository expenseCategoryRepository,
            BillPaymentRepository billPaymentRepository) {
        this.vendorBillRepository = vendorBillRepository;
        this.vendorRepository = vendorRepository;
        this.expenseCategoryRepository = expenseCategoryRepository;
        this.billPaymentRepository = billPaymentRepository;
    }

    private VendorBillResponse toResponse(VendorBill bill) {
        String vendorName = vendorRepository.findById(bill.getVendorId()).map(Vendor::getName).orElse("");
        String categoryName = expenseCategoryRepository.findById(bill.getExpenseCategoryId())
                .map(ExpenseCategory::getName).orElse("");
        long paid = billPaymentRepository.sumAmountPaiseByVendorBillId(bill.getId());
        return VendorBillResponse.from(bill, vendorName, categoryName, paid);
    }

    @GetMapping
    public List<VendorBillResponse> listVendorBills(@RequestParam(required = false) String month) {
        List<VendorBill> bills = month != null
                ? vendorBillRepository.findByBillDateStartingWith(month)
                : vendorBillRepository.findAll();
        return bills.stream().map(this::toResponse).toList();
    }

    @PostMapping
    public ResponseEntity<VendorBillResponse> createVendorBill(@Valid @RequestBody VendorBillRequest body) {
        Vendor vendor = vendorRepository.findById(body.vendorId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown vendor or expense category"));
        ExpenseCategory category = expenseCategoryRepository.findById(body.expenseCategoryId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Unknown vendor or expense category"));

        VendorBill bill = new VendorBill();
        bill.setVendorId(vendor.getId());
        bill.setExpenseCategoryId(category.getId());
        bill.setBillNumber(body.billNumber());
        bill.setBillDate(body.billDate());
        bill.setAmountPaise(body.amountPaise());
        bill.setDescription(body.description());
        bill = vendorBillRepository.save(bill);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(VendorBillResponse.from(bill, vendor.getName(), category.getName(), 0));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteVendorBill(@PathVariable Long id) {
        if (!vendorBillRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Vendor bill not found");
        }
        if (billPaymentRepository.existsByVendorBillId(id)) {
            throw new ApiException(HttpStatus.CONFLICT, "Payments have already been recorded against this bill");
        }
        vendorBillRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
