package com.pavilion.api.controller;

import com.pavilion.api.entity.UploadedFile;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.UploadedFileRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

// Serves uploaded photo bytes back out of the database — see FileStorageService for why they're
// stored there rather than on local disk. Requires being signed in: unlike the old UUID-named
// files on disk, a numeric database id is trivially enumerable, so this isn't left public.
//
// The id is a query param rather than a path variable so that Render's static-site rewrite rule
// for this endpoint can be an exact-match proxy (no wildcard, no :splat) — Render's rewrite engine
// proved unreliable at substituting :splat into the destination for this route.
@RestController
public class UploadedFileController {

    private final UploadedFileRepository uploadedFileRepository;

    public UploadedFileController(UploadedFileRepository uploadedFileRepository) {
        this.uploadedFileRepository = uploadedFileRepository;
    }

    @GetMapping("/api/uploads")
    public ResponseEntity<byte[]> getFile(@RequestParam Long id) {
        UploadedFile file = uploadedFileRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found"));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePrivate().immutable())
                .body(file.getData());
    }
}
