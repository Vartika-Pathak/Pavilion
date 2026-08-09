package com.pavilion.api.repository;

import com.pavilion.api.entity.VendorBill;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface VendorBillRepository extends JpaRepository<VendorBill, Long> {
    List<VendorBill> findByBillDateStartingWith(String monthPrefix);
}
