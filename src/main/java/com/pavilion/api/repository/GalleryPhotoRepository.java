package com.pavilion.api.repository;

import com.pavilion.api.entity.GalleryPhoto;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GalleryPhotoRepository extends JpaRepository<GalleryPhoto, Long> {
    List<GalleryPhoto> findAllByOrderByUploadedAtDesc();
}
