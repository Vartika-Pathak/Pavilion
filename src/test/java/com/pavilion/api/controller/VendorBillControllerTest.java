package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.BillPayment;
import com.pavilion.api.entity.ExpenseCategory;
import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Vendor;
import com.pavilion.api.entity.VendorBill;
import com.pavilion.api.repository.BillPaymentRepository;
import com.pavilion.api.repository.ExpenseCategoryRepository;
import com.pavilion.api.repository.VendorBillRepository;
import com.pavilion.api.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class VendorBillControllerTest extends AbstractIntegrationTest {

    @Autowired
    private VendorRepository vendorRepository;
    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;
    @Autowired
    private VendorBillRepository vendorBillRepository;
    @Autowired
    private BillPaymentRepository billPaymentRepository;

    private Vendor createVendor() {
        Vendor vendor = new Vendor();
        vendor.setName("ABC Housekeeping");
        vendor.setContactPersonName("Ravi");
        vendor.setContactNumber("9876543210");
        return vendorRepository.save(vendor);
    }

    private ExpenseCategory createCategory() {
        ExpenseCategory category = new ExpenseCategory();
        category.setName("Housekeeping");
        category.setGstSlabPercent(18);
        return expenseCategoryRepository.save(category);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/vendor-bills")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateAndListBillsWithUnpaidStatus() throws Exception {
        User admin = createUser("admin");
        Vendor vendor = createVendor();
        ExpenseCategory category = createCategory();

        mockMvc.perform(post("/api/vendor-bills")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"vendorId\":" + vendor.getId() + ",\"expenseCategoryId\":" + category.getId()
                                + ",\"billNumber\":\"INV-001\",\"billDate\":\"2026-08-01\",\"amountPaise\":1000000}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.vendorName").value("ABC Housekeeping"))
                .andExpect(jsonPath("$.status").value("unpaid"))
                .andExpect(jsonPath("$.paidAmountPaise").value(0));

        mockMvc.perform(get("/api/vendor-bills").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].billNumber").value("INV-001"));
    }

    @Test
    void statusReflectsPartialAndFullPayments() throws Exception {
        Vendor vendor = createVendor();
        ExpenseCategory category = createCategory();
        VendorBill bill = new VendorBill();
        bill.setVendorId(vendor.getId());
        bill.setExpenseCategoryId(category.getId());
        bill.setBillNumber("INV-001");
        bill.setBillDate("2026-08-01");
        bill.setAmountPaise(1000000L);
        bill = vendorBillRepository.save(bill);

        BillPayment payment = new BillPayment();
        payment.setVendorBillId(bill.getId());
        payment.setAmountPaise(400000L);
        payment.setPaymentDate("2026-08-05");
        payment.setPaymentMode("upi");
        billPaymentRepository.save(payment);

        User admin = createUser("admin");
        mockMvc.perform(get("/api/vendor-bills").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("partially_paid"))
                .andExpect(jsonPath("$[0].paidAmountPaise").value(400000));
    }

    @Test
    void monthFilterOnlyReturnsMatchingBills() throws Exception {
        Vendor vendor = createVendor();
        ExpenseCategory category = createCategory();
        VendorBill augustBill = new VendorBill();
        augustBill.setVendorId(vendor.getId());
        augustBill.setExpenseCategoryId(category.getId());
        augustBill.setBillNumber("AUG-1");
        augustBill.setBillDate("2026-08-10");
        augustBill.setAmountPaise(100000L);
        vendorBillRepository.save(augustBill);

        VendorBill julyBill = new VendorBill();
        julyBill.setVendorId(vendor.getId());
        julyBill.setExpenseCategoryId(category.getId());
        julyBill.setBillNumber("JUL-1");
        julyBill.setBillDate("2026-07-10");
        julyBill.setAmountPaise(100000L);
        vendorBillRepository.save(julyBill);

        User admin = createUser("admin");
        mockMvc.perform(get("/api/vendor-bills?month=2026-08").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].billNumber").value("AUG-1"));
    }

    @Test
    void deletingABillWithPaymentsFails() throws Exception {
        Vendor vendor = createVendor();
        ExpenseCategory category = createCategory();
        VendorBill bill = new VendorBill();
        bill.setVendorId(vendor.getId());
        bill.setExpenseCategoryId(category.getId());
        bill.setBillNumber("INV-001");
        bill.setBillDate("2026-08-01");
        bill.setAmountPaise(1000000L);
        bill = vendorBillRepository.save(bill);

        BillPayment payment = new BillPayment();
        payment.setVendorBillId(bill.getId());
        payment.setAmountPaise(400000L);
        payment.setPaymentDate("2026-08-05");
        payment.setPaymentMode("upi");
        billPaymentRepository.save(payment);

        User admin = createUser("admin");
        mockMvc.perform(delete("/api/vendor-bills/" + bill.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isConflict());
    }

    @Test
    void creatingABillForAnUnknownVendorFails() throws Exception {
        User admin = createUser("admin");
        ExpenseCategory category = createCategory();

        mockMvc.perform(post("/api/vendor-bills")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"vendorId\":999999,\"expenseCategoryId\":" + category.getId()
                                + ",\"billNumber\":\"INV-001\",\"billDate\":\"2026-08-01\",\"amountPaise\":1000000}"))
                .andExpect(status().isNotFound());
    }
}
