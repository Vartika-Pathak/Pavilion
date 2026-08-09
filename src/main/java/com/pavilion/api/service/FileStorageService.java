package com.pavilion.api.service;

import com.pavilion.api.entity.UploadedFile;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.UploadedFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Stores uploaded images as rows in the database (see UploadedFile) rather than on local disk —
// Render's disk is ephemeral and wiped on every redeploy/restart, but the database is persistent.
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_FILE_SIZE_BYTES = 8L * 1024 * 1024; // 8MB
    private static final int MAX_FILES_PER_REQUEST = 6;

    private final UploadedFileRepository uploadedFileRepository;

    public FileStorageService(UploadedFileRepository uploadedFileRepository) {
        this.uploadedFileRepository = uploadedFileRepository;
    }

    /** Validates and saves each file, returning "/api/uploads?id={id}" URLs in the same order. */
    public List<String> storeImages(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        if (files.size() > MAX_FILES_PER_REQUEST) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "At most " + MAX_FILES_PER_REQUEST + " photos are allowed");
        }

        List<String> urls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            if (file.getSize() > MAX_FILE_SIZE_BYTES) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Each photo must be 8MB or smaller");
            }
            String contentType = file.getContentType();
            if (contentType == null || !ALLOWED_IMAGE_MIME_TYPES.contains(contentType)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Only JPEG, PNG, WebP, or GIF images are allowed");
            }

            UploadedFile uploaded = new UploadedFile();
            uploaded.setFilename(file.getOriginalFilename() != null ? file.getOriginalFilename() : "upload");
            uploaded.setContentType(contentType);
            uploaded.setSize(file.getSize());
            try {
                uploaded.setData(file.getBytes());
            } catch (IOException e) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save uploaded file");
            }
            uploaded = uploadedFileRepository.save(uploaded);
            urls.add("/api/uploads?id=" + uploaded.getId());
        }
        return urls;
    }

    /** Best-effort cleanup when a request fails validation after files were already received. */
    public void deleteByUrls(List<String> urls) {
        if (urls == null) {
            return;
        }
        for (String url : urls) {
            String idPart = url.substring(url.lastIndexOf('=') + 1);
            try {
                uploadedFileRepository.deleteById(Long.parseLong(idPart));
            } catch (RuntimeException e) {
                log.warn("Failed to clean up uploaded file {}", url, e);
            }
        }
    }
}
