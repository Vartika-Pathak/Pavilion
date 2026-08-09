package com.pavilion.api.controller;

import com.pavilion.api.dto.ContentPagesDtos.CommunityStats;
import com.pavilion.api.repository.AppEventRepository;
import com.pavilion.api.repository.GalleryPhotoRepository;
import com.pavilion.api.repository.MemberRepository;
import com.pavilion.api.repository.NewsPostRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final MemberRepository memberRepository;
    private final AppEventRepository appEventRepository;
    private final NewsPostRepository newsPostRepository;
    private final GalleryPhotoRepository galleryPhotoRepository;

    public StatsController(
            MemberRepository memberRepository,
            AppEventRepository appEventRepository,
            NewsPostRepository newsPostRepository,
            GalleryPhotoRepository galleryPhotoRepository) {
        this.memberRepository = memberRepository;
        this.appEventRepository = appEventRepository;
        this.newsPostRepository = newsPostRepository;
        this.galleryPhotoRepository = galleryPhotoRepository;
    }

    @GetMapping
    public CommunityStats getStats() {
        long totalMembers = memberRepository.count();
        long upcomingEventsCount = appEventRepository.countByEventDateGreaterThanEqual(Instant.now());
        long totalNewsPosts = newsPostRepository.count();
        long totalGalleryPhotos = galleryPhotoRepository.count();
        return new CommunityStats(totalMembers, upcomingEventsCount, totalNewsPosts, totalGalleryPhotos);
    }
}
