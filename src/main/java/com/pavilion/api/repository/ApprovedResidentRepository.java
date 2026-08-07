package com.pavilion.api.repository;

import com.pavilion.api.entity.ApprovedResident;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApprovedResidentRepository extends JpaRepository<ApprovedResident, Long> {
    boolean existsByFlatNumberIgnoreCaseAndNameIgnoreCase(String flatNumber, String name);
}
