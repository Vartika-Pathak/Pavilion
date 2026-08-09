package com.pavilion.api.controller;

import com.pavilion.api.entity.UploadedFile;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.UploadedFileRepository;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

// Serves uploaded photo bytes back out of the database — see FileStorageService for why they're
// stored there rather than on local disk. Requires being signed in: unlike the old UUID-named
// files on disk, a numeric database id is trivially enumerable, so this isn't left public.
@RestController
public class UploadedFileController {

    private final UploadedFileRepository uploadedFileRepository;

    public UploadedFileController(UploadedFileRepository uploadedFileRepository) {
        this.uploadedFileRepository = uploadedFileRepository;
    }

    @GetMapping("/uploads/{id}")
    public ResponseEntity<byte[]> getFile(@PathVariable Long id) {
        UploadedFile file = uploadedFileRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "File not found"));

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePrivate().immutable())
                .body(file.getData());
    }
}
