package com.pavilion.api.controller;

import com.pavilion.api.dto.ContentPagesDtos.ResidentMeetingResponse;
import com.pavilion.api.repository.ResidentMeetingRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

// Only ever returns upcoming meetings — once a meeting's date has passed there's nothing left
// for a resident to act on, so it drops off rather than lingering in a "past" list. Read-only:
// no admin write API existed in the Node version either.
@RestController
@RequestMapping("/api/resident-meetings")
public class ResidentMeetingController {

    private final ResidentMeetingRepository residentMeetingRepository;

    public ResidentMeetingController(ResidentMeetingRepository residentMeetingRepository) {
        this.residentMeetingRepository = residentMeetingRepository;
    }

    @GetMapping
    public List<ResidentMeetingResponse> listUpcomingMeetings() {
        return residentMeetingRepository.findByMeetingDateGreaterThanEqualOrderByMeetingDateAsc(Instant.now())
                .stream().map(ResidentMeetingResponse::from).toList();
    }
}
