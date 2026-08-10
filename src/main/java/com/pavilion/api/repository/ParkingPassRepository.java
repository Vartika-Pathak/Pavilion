package com.pavilion.api.repository;

import com.pavilion.api.entity.ParkingPass;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParkingPassRepository extends JpaRepository<ParkingPass, Long> {
    Optional<ParkingPass> findByFlatNumber(String flatNumber);

    Optional<ParkingPass> findByStripeSessionId(String stripeSessionId);
}
