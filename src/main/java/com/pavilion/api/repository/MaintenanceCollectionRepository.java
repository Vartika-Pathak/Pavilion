package com.pavilion.api.repository;

import com.pavilion.api.entity.MaintenanceCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface MaintenanceCollectionRepository extends JpaRepository<MaintenanceCollection, Long> {
    List<MaintenanceCollection> findByFlatId(Long flatId);

    List<MaintenanceCollection> findByForMonth(String forMonth);

    List<MaintenanceCollection> findByFlatIdAndForMonth(Long flatId, String forMonth);

    boolean existsByFlatIdAndForMonth(Long flatId, String forMonth);
}
