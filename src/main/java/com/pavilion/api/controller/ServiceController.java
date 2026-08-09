package com.pavilion.api.controller;

import com.pavilion.api.dto.NoticesRulesServicesDtos.ServiceRequest;
import com.pavilion.api.dto.NoticesRulesServicesDtos.ServiceResponse;
import com.pavilion.api.entity.Service;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.ServiceRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/services")
@PreAuthorize("hasRole('ADMIN')")
public class ServiceController {

    private final ServiceRepository serviceRepository;

    public ServiceController(ServiceRepository serviceRepository) {
        this.serviceRepository = serviceRepository;
    }

    @GetMapping
    public List<ServiceResponse> listServices() {
        return serviceRepository.findAll().stream().map(ServiceResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody ServiceRequest body) {
        Service service = new Service();
        applyRequest(service, body);
        service = serviceRepository.save(service);
        return ResponseEntity.status(HttpStatus.CREATED).body(ServiceResponse.from(service));
    }

    @PutMapping("/{id}")
    public ServiceResponse updateService(@PathVariable Long id, @Valid @RequestBody ServiceRequest body) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Service not found"));
        applyRequest(service, body);
        return ServiceResponse.from(serviceRepository.save(service));
    }

    private void applyRequest(Service service, ServiceRequest body) {
        service.setName(body.name());
        service.setCategory(body.category());
        service.setContactNumber(body.contactNumber());
        service.setNotes(body.notes());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        if (!serviceRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Service not found");
        }
        serviceRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
