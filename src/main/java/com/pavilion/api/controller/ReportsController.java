package com.pavilion.api.controller;

import com.pavilion.api.dto.ReportsDtos.BalanceSheet;
import com.pavilion.api.dto.ReportsDtos.DashboardSummary;
import com.pavilion.api.dto.ReportsDtos.DueListEntry;
import com.pavilion.api.dto.ReportsDtos.IncomeStatement;
import com.pavilion.api.dto.ReportsDtos.MaintenanceSummaryByFlatType;
import com.pavilion.api.dto.ReportsDtos.MonthlyTrendPoint;
import com.pavilion.api.entity.Building;
import com.pavilion.api.entity.Flat;
import com.pavilion.api.entity.MaintenanceCollection;
import com.pavilion.api.entity.MaintenanceRate;
import com.pavilion.api.entity.VendorBill;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.BillPaymentRepository;
import com.pavilion.api.repository.BuildingRepository;
import com.pavilion.api.repository.FlatRepository;
import com.pavilion.api.repository.MaintenanceCollectionRepository;
import com.pavilion.api.repository.MaintenanceRateRepository;
import com.pavilion.api.repository.VendorBillRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("hasRole('ADMIN')")
public class ReportsController {

    private static final List<String> FLAT_TYPES = List.of("1bhk", "2bhk", "3bhk", "4bhk");
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    private final BuildingRepository buildingRepository;
    private final FlatRepository flatRepository;
    private final MaintenanceRateRepository maintenanceRateRepository;
    private final MaintenanceCollectionRepository maintenanceCollectionRepository;
    private final VendorBillRepository vendorBillRepository;
    private final BillPaymentRepository billPaymentRepository;

    public ReportsController(
            BuildingRepository buildingRepository,
            FlatRepository flatRepository,
            MaintenanceRateRepository maintenanceRateRepository,
            MaintenanceCollectionRepository maintenanceCollectionRepository,
            VendorBillRepository vendorBillRepository,
            BillPaymentRepository billPaymentRepository) {
        this.buildingRepository = buildingRepository;
        this.flatRepository = flatRepository;
        this.maintenanceRateRepository = maintenanceRateRepository;
        this.maintenanceCollectionRepository = maintenanceCollectionRepository;
        this.vendorBillRepository = vendorBillRepository;
        this.billPaymentRepository = billPaymentRepository;
    }

    private static String currentMonth() {
        return LocalDate.now(ZoneOffset.UTC).format(MONTH_FORMAT);
    }

    // Trailing N months ending at (and including) the current month, oldest first — e.g.
    // count=6 in August gives ["2026-03", ..., "2026-08"].
    private static List<String> trailingMonths(int count) {
        List<String> months = new ArrayList<>();
        LocalDate now = LocalDate.now(ZoneOffset.UTC);
        for (int i = count - 1; i >= 0; i--) {
            months.add(now.minusMonths(i).format(MONTH_FORMAT));
        }
        return months;
    }

    private long allTimeIncome() {
        return maintenanceCollectionRepository.sumAllAmountPaise();
    }

    private long allTimeExpense() {
        return billPaymentRepository.sumAllAmountPaise();
    }

    private long totalPayables() {
        long total = 0;
        for (VendorBill bill : vendorBillRepository.findAll()) {
            long paid = billPaymentRepository.sumAmountPaiseByVendorBillId(bill.getId());
            total += Math.max(bill.getAmountPaise() - paid, 0);
        }
        return total;
    }

    private List<DueListEntry> dueListForMonth(String month) {
        List<Flat> flats = flatRepository.findAll();
        Map<Long, String> buildingNamesById = new HashMap<>();
        for (Building building : buildingRepository.findAll()) {
            buildingNamesById.put(building.getId(), building.getName());
        }
        Map<String, Long> rateByFlatType = new HashMap<>();
        for (MaintenanceRate rate : maintenanceRateRepository.findAll()) {
            rateByFlatType.put(rate.getFlatType(), rate.getMonthlyAmountPaise());
        }
        Map<Long, Long> collectedByFlatId = new HashMap<>();
        for (MaintenanceCollection collection : maintenanceCollectionRepository.findByForMonth(month)) {
            collectedByFlatId.merge(collection.getFlatId(), collection.getAmountPaise(), Long::sum);
        }

        List<DueListEntry> entries = new ArrayList<>();
        for (Flat flat : flats) {
            long expected = rateByFlatType.getOrDefault(flat.getFlatType(), 0L);
            long collected = collectedByFlatId.getOrDefault(flat.getId(), 0L);
            entries.add(new DueListEntry(
                    flat.getId(), buildingNamesById.get(flat.getBuildingId()), flat.getFlatNumber(), flat.getFlatType(),
                    expected, collected, Math.max(expected - collected, 0)));
        }
        return entries;
    }

    @GetMapping("/due-list")
    public List<DueListEntry> getDueList(@RequestParam(required = false) String month) {
        if (month == null || month.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "month is required (YYYY-MM)");
        }
        return dueListForMonth(month);
    }

    @GetMapping("/dashboard-summary")
    public DashboardSummary getDashboardSummary() {
        String month = currentMonth();
        List<String> months = trailingMonths(6);

        long income = maintenanceCollectionRepository.sumAmountPaiseByForMonth(month);
        long expense = billPaymentRepository.sumAmountPaiseByMonth(month);
        long cashIn = allTimeIncome();
        long cashOut = allTimeExpense();
        List<DueListEntry> dueList = dueListForMonth(month);

        Map<String, Long> countByFlatType = new HashMap<>();
        for (Flat flat : flatRepository.findAll()) {
            countByFlatType.merge(flat.getFlatType(), 1L, Long::sum);
        }
        Map<String, Long> rateByFlatType = new HashMap<>();
        for (MaintenanceRate rate : maintenanceRateRepository.findAll()) {
            rateByFlatType.put(rate.getFlatType(), rate.getMonthlyAmountPaise());
        }
        Map<String, Long> collectedByFlatType = new HashMap<>();
        for (DueListEntry entry : dueList) {
            collectedByFlatType.merge(entry.flatType(), entry.collectedAmountPaise(), Long::sum);
        }

        List<MaintenanceSummaryByFlatType> maintenanceSummary = new ArrayList<>();
        for (String flatType : FLAT_TYPES) {
            long totalFlats = countByFlatType.getOrDefault(flatType, 0L);
            long monthlyAmountPaise = rateByFlatType.getOrDefault(flatType, 0L);
            maintenanceSummary.add(new MaintenanceSummaryByFlatType(
                    flatType, totalFlats, monthlyAmountPaise, totalFlats * monthlyAmountPaise,
                    collectedByFlatType.getOrDefault(flatType, 0L)));
        }

        List<MonthlyTrendPoint> monthlyTrend = new ArrayList<>();
        for (String m : months) {
            monthlyTrend.add(new MonthlyTrendPoint(
                    m, maintenanceCollectionRepository.sumAmountPaiseByForMonth(m), billPaymentRepository.sumAmountPaiseByMonth(m)));
        }

        long totalDue = 0;
        for (DueListEntry entry : dueList) {
            totalDue += entry.dueAmountPaise();
        }

        return new DashboardSummary(cashIn - cashOut, income, expense, totalDue, maintenanceSummary, monthlyTrend);
    }

    @GetMapping("/balance-sheet")
    public BalanceSheet getBalanceSheet() {
        long totalCollected = allTimeIncome();
        long totalPaidToVendors = allTimeExpense();
        return new BalanceSheet(totalCollected, totalPaidToVendors, totalCollected - totalPaidToVendors, totalPayables());
    }

    @GetMapping("/income-statement")
    public IncomeStatement getIncomeStatement(
            @RequestParam(required = false) String from, @RequestParam(required = false) String to) {
        if (from == null || from.isBlank() || to == null || to.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "from and to are required (YYYY-MM-DD)");
        }

        long incomePaise = maintenanceCollectionRepository.sumAmountPaiseBetween(from, to);
        long expensePaise = billPaymentRepository.sumAmountPaiseBetween(from, to);
        return new IncomeStatement(from, to, incomePaise, expensePaise, incomePaise - expensePaise);
    }

    @GetMapping("/income-vs-expense-trend")
    public List<MonthlyTrendPoint> getIncomeVsExpenseTrend(@RequestParam(required = false) Integer months) {
        List<String> targetMonths = trailingMonths(months != null ? months : 6);
        List<MonthlyTrendPoint> trend = new ArrayList<>();
        for (String m : targetMonths) {
            trend.add(new MonthlyTrendPoint(
                    m, maintenanceCollectionRepository.sumAmountPaiseByForMonth(m), billPaymentRepository.sumAmountPaiseByMonth(m)));
        }
        return trend;
    }
}
