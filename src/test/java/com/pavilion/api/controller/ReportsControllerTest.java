package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.BillPayment;
import com.pavilion.api.entity.Building;
import com.pavilion.api.entity.ExpenseCategory;
import com.pavilion.api.entity.Flat;
import com.pavilion.api.entity.MaintenanceCollection;
import com.pavilion.api.entity.MaintenanceRate;
import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Vendor;
import com.pavilion.api.entity.VendorBill;
import com.pavilion.api.repository.BillPaymentRepository;
import com.pavilion.api.repository.BuildingRepository;
import com.pavilion.api.repository.ExpenseCategoryRepository;
import com.pavilion.api.repository.FlatRepository;
import com.pavilion.api.repository.MaintenanceCollectionRepository;
import com.pavilion.api.repository.MaintenanceRateRepository;
import com.pavilion.api.repository.VendorBillRepository;
import com.pavilion.api.repository.VendorRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportsControllerTest extends AbstractIntegrationTest {

    @Autowired
    private BuildingRepository buildingRepository;
    @Autowired
    private FlatRepository flatRepository;
    @Autowired
    private MaintenanceRateRepository maintenanceRateRepository;
    @Autowired
    private MaintenanceCollectionRepository maintenanceCollectionRepository;
    @Autowired
    private VendorRepository vendorRepository;
    @Autowired
    private ExpenseCategoryRepository expenseCategoryRepository;
    @Autowired
    private VendorBillRepository vendorBillRepository;
    @Autowired
    private BillPaymentRepository billPaymentRepository;

    private static String currentMonth() {
        return LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("yyyy-MM"));
    }

    private Flat createFlat(String flatNumber, String flatType) {
        Building building = new Building();
        building.setName("Tower A");
        building.setTotalFlats(10);
        building = buildingRepository.save(building);

        Flat flat = new Flat();
        flat.setBuildingId(building.getId());
        flat.setFlatNumber(flatNumber);
        flat.setFlatType(flatType);
        flat.setOccupied(true);
        flat.setOwnershipType("owner");
        return flatRepository.save(flat);
    }

    @Test
    void dueListRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/reports/due-list?month=2026-08")).andExpect(status().isUnauthorized());
    }

    @Test
    void dueListRequiresAMonthParam() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(get("/api/reports/due-list").cookie(sessionCookie(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void dueListReflectsExpectedVsCollected() throws Exception {
        User admin = createUser("admin");
        Flat flat = createFlat("A-1", "2bhk");

        MaintenanceRate rate = new MaintenanceRate();
        rate.setFlatType("2bhk");
        rate.setMonthlyAmountPaise(500000L);
        maintenanceRateRepository.save(rate);

        String month = currentMonth();
        MaintenanceCollection collection = new MaintenanceCollection();
        collection.setFlatId(flat.getId());
        collection.setPayerName("Resident A");
        collection.setAmountPaise(200000L);
        collection.setPaymentDate(month + "-05");
        collection.setPaymentMode("upi");
        collection.setForMonth(month);
        maintenanceCollectionRepository.save(collection);

        mockMvc.perform(get("/api/reports/due-list?month=" + month).cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].expectedAmountPaise").value(500000))
                .andExpect(jsonPath("$[0].collectedAmountPaise").value(200000))
                .andExpect(jsonPath("$[0].dueAmountPaise").value(300000));
    }

    @Test
    void dashboardSummaryAggregatesAcrossFlatTypes() throws Exception {
        User admin = createUser("admin");
        Flat flat = createFlat("A-1", "2bhk");

        MaintenanceRate rate = new MaintenanceRate();
        rate.setFlatType("2bhk");
        rate.setMonthlyAmountPaise(500000L);
        maintenanceRateRepository.save(rate);

        String month = currentMonth();
        MaintenanceCollection collection = new MaintenanceCollection();
        collection.setFlatId(flat.getId());
        collection.setPayerName("Resident A");
        collection.setAmountPaise(500000L);
        collection.setPaymentDate(month + "-05");
        collection.setPaymentMode("upi");
        collection.setForMonth(month);
        maintenanceCollectionRepository.save(collection);

        mockMvc.perform(get("/api/reports/dashboard-summary").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCollectedThisMonthPaise").value(500000))
                .andExpect(jsonPath("$.totalDueThisMonthPaise").value(0))
                .andExpect(jsonPath("$.cashBalancePaise").value(500000))
                .andExpect(jsonPath("$.monthlyTrend.length()").value(6));
    }

    @Test
    void balanceSheetComputesPayablesFromUnpaidBills() throws Exception {
        User admin = createUser("admin");

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
        bill.setBillNumber("INV-1");
        bill.setBillDate("2026-08-01");
        bill.setAmountPaise(1000000L);
        bill = vendorBillRepository.save(bill);

        BillPayment payment = new BillPayment();
        payment.setVendorBillId(bill.getId());
        payment.setAmountPaise(400000L);
        payment.setPaymentDate("2026-08-05");
        payment.setPaymentMode("upi");
        billPaymentRepository.save(payment);

        mockMvc.perform(get("/api/reports/balance-sheet").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalPaidToVendorsPaise").value(400000))
                .andExpect(jsonPath("$.totalPayablesPaise").value(600000));
    }

    @Test
    void incomeStatementRequiresFromAndTo() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(get("/api/reports/income-statement").cookie(sessionCookie(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void incomeStatementSumsWithinDateRange() throws Exception {
        User admin = createUser("admin");
        Flat flat = createFlat("A-1", "2bhk");

        MaintenanceCollection inRange = new MaintenanceCollection();
        inRange.setFlatId(flat.getId());
        inRange.setPayerName("Resident A");
        inRange.setAmountPaise(500000L);
        inRange.setPaymentDate("2026-08-05");
        inRange.setPaymentMode("upi");
        inRange.setForMonth("2026-08");
        maintenanceCollectionRepository.save(inRange);

        MaintenanceCollection outOfRange = new MaintenanceCollection();
        outOfRange.setFlatId(flat.getId());
        outOfRange.setPayerName("Resident A");
        outOfRange.setAmountPaise(999999L);
        outOfRange.setPaymentDate("2026-01-05");
        outOfRange.setPaymentMode("upi");
        outOfRange.setForMonth("2026-01");
        maintenanceCollectionRepository.save(outOfRange);

        mockMvc.perform(get("/api/reports/income-statement?from=2026-08-01&to=2026-08-31").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incomePaise").value(500000))
                .andExpect(jsonPath("$.netPaise").value(500000));
    }

    @Test
    void incomeVsExpenseTrendDefaultsToSixMonths() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(get("/api/reports/income-vs-expense-trend").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(6))
                .andExpect(jsonPath("$[5].month").value(currentMonth()));
    }

    @Test
    void incomeVsExpenseTrendRespectsMonthsParam() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(get("/api/reports/income-vs-expense-trend?months=3").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }
}
