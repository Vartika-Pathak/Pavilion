package com.pavilion.api.controller;

import com.pavilion.api.dto.NoticesRulesServicesDtos.SocietyRuleRequest;
import com.pavilion.api.dto.NoticesRulesServicesDtos.SocietyRuleResponse;
import com.pavilion.api.entity.SocietyRule;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.SocietyRuleRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/society-rules")
@PreAuthorize("hasRole('ADMIN')")
public class SocietyRuleController {

    private final SocietyRuleRepository societyRuleRepository;

    public SocietyRuleController(SocietyRuleRepository societyRuleRepository) {
        this.societyRuleRepository = societyRuleRepository;
    }

    @GetMapping
    public List<SocietyRuleResponse> listSocietyRules() {
        return societyRuleRepository.findAll().stream().map(SocietyRuleResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<SocietyRuleResponse> createSocietyRule(@Valid @RequestBody SocietyRuleRequest body) {
        SocietyRule rule = new SocietyRule();
        applyRequest(rule, body);
        rule = societyRuleRepository.save(rule);
        return ResponseEntity.status(HttpStatus.CREATED).body(SocietyRuleResponse.from(rule));
    }

    @PutMapping("/{id}")
    public SocietyRuleResponse updateSocietyRule(@PathVariable Long id, @Valid @RequestBody SocietyRuleRequest body) {
        SocietyRule rule = societyRuleRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Society rule not found"));
        applyRequest(rule, body);
        return SocietyRuleResponse.from(societyRuleRepository.save(rule));
    }

    private void applyRequest(SocietyRule rule, SocietyRuleRequest body) {
        rule.setTitle(body.title());
        rule.setDescription(body.description());
        rule.setActive(body.active());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSocietyRule(@PathVariable Long id) {
        if (!societyRuleRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Society rule not found");
        }
        societyRuleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
