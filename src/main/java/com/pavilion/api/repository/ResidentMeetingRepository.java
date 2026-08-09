package com.pavilion.api.repository;

import com.pavilion.api.entity.ResidentMeeting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface ResidentMeetingRepository extends JpaRepository<ResidentMeeting, Long> {
    List<ResidentMeeting> findByMeetingDateGreaterThanEqualOrderByMeetingDateAsc(Instant from);
}
