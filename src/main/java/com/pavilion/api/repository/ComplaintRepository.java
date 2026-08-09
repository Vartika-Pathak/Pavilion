package com.pavilion.api.repository;

import com.pavilion.api.entity.Complaint;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findAllByOrderByCreatedAtDesc();

    List<Complaint> findByResidentIdOrderByCreatedAtDesc(Long residentId);
}
