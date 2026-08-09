package com.pavilion.api.controller;

import com.pavilion.api.dto.ContentPagesDtos.GalleryPhotoRequest;
import com.pavilion.api.dto.ContentPagesDtos.GalleryPhotoResponse;
import com.pavilion.api.entity.GalleryPhoto;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.GalleryPhotoRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Reads are public; writes are admin-only — the Node version's POST/DELETE had no auth check
// at all, same oversight as Events, closed the same way here.
@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    private final GalleryPhotoRepository galleryPhotoRepository;

    public GalleryController(GalleryPhotoRepository galleryPhotoRepository) {
        this.galleryPhotoRepository = galleryPhotoRepository;
    }

    @GetMapping
    public List<GalleryPhotoResponse> listPhotos() {
        return galleryPhotoRepository.findAllByOrderByUploadedAtDesc().stream().map(GalleryPhotoResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GalleryPhotoResponse> addPhoto(@Valid @RequestBody GalleryPhotoRequest body) {
        GalleryPhoto photo = new GalleryPhoto();
        photo.setImageUrl(body.imageUrl());
        photo.setTitle(body.title());
        photo.setDescription(body.description());
        photo.setUploadedBy(body.uploadedBy());
        photo = galleryPhotoRepository.save(photo);
        return ResponseEntity.status(HttpStatus.CREATED).body(GalleryPhotoResponse.from(photo));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
        if (!galleryPhotoRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Photo not found");
        }
        galleryPhotoRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
