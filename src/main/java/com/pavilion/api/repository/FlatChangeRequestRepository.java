package com.pavilion.api.repository;

import com.pavilion.api.entity.FlatChangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FlatChangeRequestRepository extends JpaRepository<FlatChangeRequest, Long> {
    List<FlatChangeRequest> findAllByOrderByCreatedAtDesc();
}
