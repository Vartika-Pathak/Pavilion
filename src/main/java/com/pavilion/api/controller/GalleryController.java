package com.pavilion.api.controller;

import com.pavilion.api.dto.ContentPagesDtos.GalleryPhotoResponse;
import com.pavilion.api.entity.GalleryPhoto;
import com.pavilion.api.entity.User;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.GalleryPhotoRepository;
import com.pavilion.api.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

// Reads are public; writes are admin-only — the Node version's POST/DELETE had no auth check
// at all, same oversight as Events, closed the same way here.
@RestController
@RequestMapping("/api/gallery")
public class GalleryController {

    private static final String UPLOADED_FILE_URL_PREFIX = "/api/uploads?id=";

    private final GalleryPhotoRepository galleryPhotoRepository;
    private final FileStorageService fileStorageService;

    public GalleryController(GalleryPhotoRepository galleryPhotoRepository, FileStorageService fileStorageService) {
        this.galleryPhotoRepository = galleryPhotoRepository;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public List<GalleryPhotoResponse> listPhotos() {
        return galleryPhotoRepository.findAllByOrderByUploadedAtDesc().stream().map(GalleryPhotoResponse::from).toList();
    }

    @PostMapping(consumes = "multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GalleryPhotoResponse> addPhoto(
            @AuthenticationPrincipal User admin,
            @RequestParam("photo") MultipartFile photo,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description) {
        List<String> urls = fileStorageService.storeImages(List.of(photo));
        if (urls.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "A photo is required");
        }

        GalleryPhoto entity = new GalleryPhoto();
        entity.setImageUrl(urls.get(0));
        entity.setTitle(title);
        entity.setDescription(description);
        entity.setUploadedBy(admin.getName());
        entity = galleryPhotoRepository.save(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(GalleryPhotoResponse.from(entity));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePhoto(@PathVariable Long id) {
        GalleryPhoto photo = galleryPhotoRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Photo not found"));
        galleryPhotoRepository.deleteById(id);
        // Only clean up the underlying stored file if this photo was uploaded through us — older
        // seeded photos can point at external URLs (Unsplash etc.), which deleteByUrls must never
        // be handed, since its id-parsing isn't scoped to our own upload URLs.
        if (photo.getImageUrl() != null && photo.getImageUrl().startsWith(UPLOADED_FILE_URL_PREFIX)) {
            fileStorageService.deleteByUrls(List.of(photo.getImageUrl()));
        }
        return ResponseEntity.noContent().build();
    }
}
