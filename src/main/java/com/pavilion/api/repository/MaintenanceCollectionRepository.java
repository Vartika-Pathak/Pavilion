package com.pavilion.api.repository;

import com.pavilion.api.entity.MaintenanceCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface MaintenanceCollectionRepository extends JpaRepository<MaintenanceCollection, Long> {
    List<MaintenanceCollection> findByFlatId(Long flatId);

    List<MaintenanceCollection> findByForMonth(String forMonth);

    List<MaintenanceCollection> findByFlatIdAndForMonth(Long flatId, String forMonth);

    boolean existsByFlatIdAndForMonth(Long flatId, String forMonth);

    @Query("select coalesce(sum(c.amountPaise), 0) from MaintenanceCollection c where c.forMonth = :forMonth")
    long sumAmountPaiseByForMonth(String forMonth);

    @Query("select coalesce(sum(c.amountPaise), 0) from MaintenanceCollection c")
    long sumAllAmountPaise();

    @Query("select coalesce(sum(c.amountPaise), 0) from MaintenanceCollection c where c.paymentDate >= :from and c.paymentDate <= :to")
    long sumAmountPaiseBetween(String from, String to);
}
