package com.pavilion.api.service;

import com.pavilion.api.exception.ApiException;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

// Stores uploaded images on local disk under ./uploads, served back out via WebConfig's static
// resource mapping at /uploads/**. Same convention as the Node version: on Render's free tier
// this disk is ephemeral and wiped on every redeploy — fine for now, a persistent disk would be
// needed to keep uploads around long-term.
@Service
public class FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(FileStorageService.class);

    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final long MAX_FILE_SIZE_BYTES = 8L * 1024 * 1024; // 8MB
    private static final int MAX_FILES_PER_REQUEST = 6;

    public static final String UPLOADS_DIR_NAME = "uploads";

    private Path uploadsDir;

    @PostConstruct
    void init() {
        uploadsDir = Paths.get(UPLOADS_DIR_NAME).toAbsolutePath().normalize();
        try {
            Files.createDirectories(uploadsDir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create uploads directory at " + uploadsDir, e);
        }
    }

    public Path getUploadsDir() {
        return uploadsDir;
    }

    /** Validates and saves each file, returning "/uploads/{filename}" URLs in the same order. */
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

            String extension = extensionFor(file.getOriginalFilename());
            String filename = UUID.randomUUID() + extension;
            try {
                Files.copy(file.getInputStream(), uploadsDir.resolve(filename));
            } catch (IOException e) {
                throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not save uploaded file");
            }
            urls.add("/uploads/" + filename);
        }
        return urls;
    }

    /** Best-effort cleanup when a request fails validation after files were already received. */
    public void deleteByUrls(List<String> urls) {
        if (urls == null) {
            return;
        }
        for (String url : urls) {
            String filename = url.substring(url.lastIndexOf('/') + 1);
            try {
                Files.deleteIfExists(uploadsDir.resolve(filename));
            } catch (IOException e) {
                log.warn("Failed to clean up uploaded file {}", filename, e);
            }
        }
    }

    private static String extensionFor(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dotIndex = originalFilename.lastIndexOf('.');
        return dotIndex >= 0 ? originalFilename.substring(dotIndex) : "";
    }
}
