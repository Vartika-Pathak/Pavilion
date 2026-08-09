package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.ExpenseCategory;
import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Vendor;
import com.pavilion.api.entity.VendorBill;
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

class BillPaymentControllerTest extends AbstractIntegrationTest {

    @Autowired
    private VendorRepository vendorRepository;
    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;
    @Autowired
    private VendorBillRepository vendorBillRepository;

    private VendorBill createBill() {
        Vendor vendor = new Vendor();
        vendor.setName("ABC Housekeeping");
        vendor.setContactPersonName("Ravi");
        vendor.setContactNumber("9876543210");
        vendor = vendorRepository.save(vendor);

        ExpenseCategory category = new ExpenseCategory();
        category.setName("Housekeeping");
        category.setGstSlabPercent(18);
        category = expenseCategoryRepository.save(category);

        VendorBill bill = new VendorBill();
        bill.setVendorId(vendor.getId());
        bill.setExpenseCategoryId(category.getId());
        bill.setBillNumber("INV-001");
        bill.setBillDate("2026-08-01");
        bill.setAmountPaise(1000000L);
        return vendorBillRepository.save(bill);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/bill-payments")).andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanRecordAndListPayments() throws Exception {
        User admin = createUser("admin");
        VendorBill bill = createBill();

        mockMvc.perform(post("/api/bill-payments")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"vendorBillId\":" + bill.getId() + ",\"amountPaise\":400000,\"paymentDate\":\"2026-08-05\",\"paymentMode\":\"upi\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amountPaise").value(400000))
                .andExpect(jsonPath("$.paymentMode").value("upi"));

        mockMvc.perform(get("/api/bill-payments?vendorBillId=" + bill.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].amountPaise").value(400000));
    }

    @Test
    void paymentAgainstAnUnknownBillFails() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(post("/api/bill-payments")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"vendorBillId\":999999,\"amountPaise\":400000,\"paymentDate\":\"2026-08-05\",\"paymentMode\":\"upi\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidPaymentModeFails() throws Exception {
        User admin = createUser("admin");
        VendorBill bill = createBill();
        mockMvc.perform(post("/api/bill-payments")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"vendorBillId\":" + bill.getId() + ",\"amountPaise\":400000,\"paymentDate\":\"2026-08-05\",\"paymentMode\":\"bitcoin\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanDeleteAPayment() throws Exception {
        User admin = createUser("admin");
        VendorBill bill = createBill();

        String response = mockMvc.perform(post("/api/bill-payments")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"vendorBillId\":" + bill.getId() + ",\"amountPaise\":400000,\"paymentDate\":\"2026-08-05\",\"paymentMode\":\"upi\"}"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/bill-payments/" + id).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }
}
