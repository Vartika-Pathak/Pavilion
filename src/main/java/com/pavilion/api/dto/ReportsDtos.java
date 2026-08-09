package com.pavilion.api.dto;

import java.util.List;

public class ReportsDtos {

    public record DueListEntry(
            Long flatId, String buildingName, String flatNumber, String flatType,
            long expectedAmountPaise, long collectedAmountPaise, long dueAmountPaise) {
    }

    public record MonthlyTrendPoint(String month, long incomePaise, long expensePaise) {
    }

    public record MaintenanceSummaryByFlatType(
            String flatType, long totalFlats, long monthlyAmountPaise, long expectedTotalPaise, long collectedTotalPaise) {
    }

    public record DashboardSummary(
            long cashBalancePaise, long totalCollectedThisMonthPaise, long totalExpensesThisMonthPaise,
            long totalDueThisMonthPaise, List<MaintenanceSummaryByFlatType> maintenanceSummary,
            List<MonthlyTrendPoint> monthlyTrend) {
    }

    public record BalanceSheet(
            long totalCollectedPaise, long totalPaidToVendorsPaise, long cashBalancePaise, long totalPayablesPaise) {
    }

    public record IncomeStatement(String from, String to, long incomePaise, long expensePaise, long netPaise) {
    }
}
