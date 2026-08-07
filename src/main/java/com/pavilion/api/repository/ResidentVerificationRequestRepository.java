package com.pavilion.api.repository;

import com.pavilion.api.entity.ResidentVerificationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResidentVerificationRequestRepository extends JpaRepository<ResidentVerificationRequest, Long> {
    Optional<ResidentVerificationRequest> findByFlatNumberIgnoreCaseAndNameIgnoreCase(String flatNumber, String name);

    List<ResidentVerificationRequest> findAllByOrderByStatusAscCreatedAtAsc();
}
