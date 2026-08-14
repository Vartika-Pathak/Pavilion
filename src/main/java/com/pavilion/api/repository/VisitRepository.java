package com.pavilion.api.repository;

import com.pavilion.api.entity.User;
import com.pavilion.api.entity.Visit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VisitRepository extends JpaRepository<Visit, Long> {
    List<Visit> findByResidentOrderByCreatedAtDesc(User resident);

    List<Visit> findAllByOrderByCreatedAtDesc();

    Optional<Visit> findByOtpCode(String otpCode);
}
