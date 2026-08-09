package com.pavilion.api.repository;

import com.pavilion.api.entity.AmenityBooking;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AmenityBookingRepository extends JpaRepository<AmenityBooking, Long> {

    List<AmenityBooking> findByResidentId(Long residentId);

    List<AmenityBooking> findByAmenityIdAndBookingDate(String amenityId, String bookingDate);

    Optional<AmenityBooking> findByAmenityIdAndBookingDateAndSlot(String amenityId, String bookingDate, String slot);

    Optional<AmenityBooking> findByStripeSessionId(String stripeSessionId);
}
