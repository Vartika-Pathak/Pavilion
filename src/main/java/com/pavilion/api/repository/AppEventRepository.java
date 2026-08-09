package com.pavilion.api.repository;

import com.pavilion.api.entity.AppEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface AppEventRepository extends JpaRepository<AppEvent, Long> {
    List<AppEvent> findAllByOrderByEventDateAsc();

    List<AppEvent> findTop5ByEventDateGreaterThanEqualOrderByEventDateAsc(Instant from);

    long countByEventDateGreaterThanEqual(Instant from);
}
