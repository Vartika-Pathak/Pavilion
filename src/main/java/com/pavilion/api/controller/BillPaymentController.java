package com.pavilion.api.controller;

import com.pavilion.api.dto.TransactionsDtos.BillPaymentRequest;
import com.pavilion.api.dto.TransactionsDtos.BillPaymentResponse;
import com.pavilion.api.entity.BillPayment;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.BillPaymentRepository;
import com.pavilion.api.repository.VendorBillRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bill-payments")
@PreAuthorize("hasRole('ADMIN')")
public class BillPaymentController {

    private final BillPaymentRepository billPaymentRepository;
    private final VendorBillRepository vendorBillRepository;

    public BillPaymentController(BillPaymentRepository billPaymentRepository, VendorBillRepository vendorBillRepository) {
        this.billPaymentRepository = billPaymentRepository;
        this.vendorBillRepository = vendorBillRepository;
    }

    @GetMapping
    public List<BillPaymentResponse> listBillPayments(@RequestParam(required = false) Long vendorBillId) {
        List<BillPayment> payments = vendorBillId != null
                ? billPaymentRepository.findByVendorBillId(vendorBillId)
                : billPaymentRepository.findAll();
        return payments.stream().map(BillPaymentResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<BillPaymentResponse> createBillPayment(@Valid @RequestBody BillPaymentRequest body) {
        if (!vendorBillRepository.existsById(body.vendorBillId())) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Unknown vendor bill");
        }

        BillPayment payment = new BillPayment();
        payment.setVendorBillId(body.vendorBillId());
        payment.setAmountPaise(body.amountPaise());
        payment.setPaymentDate(body.paymentDate());
        payment.setPaymentMode(body.paymentMode());
        payment.setReferenceNumber(body.referenceNumber());
        payment.setNotes(body.notes());
        payment = billPaymentRepository.save(payment);

        return ResponseEntity.status(HttpStatus.CREATED).body(BillPaymentResponse.from(payment));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBillPayment(@PathVariable Long id) {
        if (!billPaymentRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Bill payment not found");
        }
        billPaymentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
