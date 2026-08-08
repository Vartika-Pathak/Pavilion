package com.pavilion.api.controller;

import com.pavilion.api.dto.MastersDtos.SocietyInfoResponse;
import com.pavilion.api.dto.MastersDtos.UpdateSocietyInfoRequest;
import com.pavilion.api.entity.Society;
import com.pavilion.api.repository.SocietyRepository;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/society-info")
@PreAuthorize("hasRole('ADMIN')")
public class SocietyController {

    private final SocietyRepository societyRepository;

    public SocietyController(SocietyRepository societyRepository) {
        this.societyRepository = societyRepository;
    }

    // There's only ever one society row — this fetches it, creating a blank default the first
    // time anyone asks, so the admin has something to edit instead of hitting a 404.
    private Society getOrCreateSociety() {
        List<Society> existing = societyRepository.findAll();
        if (!existing.isEmpty()) {
            return existing.get(0);
        }
        return societyRepository.save(new Society());
    }

    @GetMapping
    public SocietyInfoResponse getSocietyInfo() {
        return SocietyInfoResponse.from(getOrCreateSociety());
    }

    @PutMapping
    public SocietyInfoResponse updateSocietyInfo(@Valid @RequestBody UpdateSocietyInfoRequest body) {
        Society society = getOrCreateSociety();
        society.setName(body.name());
        society.setAddress(body.address());
        society.setContactNumber(body.contactNumber());
        society.setEmail(body.email());
        return SocietyInfoResponse.from(societyRepository.save(society));
    }
}
