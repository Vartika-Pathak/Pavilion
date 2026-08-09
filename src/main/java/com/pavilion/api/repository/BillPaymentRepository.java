package com.pavilion.api.repository;

import com.pavilion.api.entity.BillPayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface BillPaymentRepository extends JpaRepository<BillPayment, Long> {
    List<BillPayment> findByVendorBillId(Long vendorBillId);

    boolean existsByVendorBillId(Long vendorBillId);

    @Query("select coalesce(sum(p.amountPaise), 0) from BillPayment p where p.vendorBillId = :vendorBillId")
    long sumAmountPaiseByVendorBillId(Long vendorBillId);

    @Query("select coalesce(sum(p.amountPaise), 0) from BillPayment p where p.paymentDate like concat(:month, '%')")
    long sumAmountPaiseByMonth(String month);

    @Query("select coalesce(sum(p.amountPaise), 0) from BillPayment p")
    long sumAllAmountPaise();

    @Query("select coalesce(sum(p.amountPaise), 0) from BillPayment p where p.paymentDate >= :from and p.paymentDate <= :to")
    long sumAmountPaiseBetween(String from, String to);
}
