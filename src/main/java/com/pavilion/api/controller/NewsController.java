package com.pavilion.api.controller;

import com.pavilion.api.dto.ContentPagesDtos.NewsPostRequest;
import com.pavilion.api.dto.ContentPagesDtos.NewsPostResponse;
import com.pavilion.api.entity.NewsPost;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.NewsPostRepository;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

// Reads are public; writes are admin-only — same oversight-closing as Gallery/Events.
@RestController
@RequestMapping("/api/news")
public class NewsController {

    private final NewsPostRepository newsPostRepository;

    public NewsController(NewsPostRepository newsPostRepository) {
        this.newsPostRepository = newsPostRepository;
    }

    @GetMapping("/latest")
    public List<NewsPostResponse> listLatestPosts() {
        return newsPostRepository.findAllByOrderByPublishedAtDesc(PageRequest.of(0, 3))
                .stream().map(NewsPostResponse::from).toList();
    }

    @GetMapping
    public List<NewsPostResponse> listPosts() {
        return newsPostRepository.findAllByOrderByPublishedAtDesc().stream().map(NewsPostResponse::from).toList();
    }

    @GetMapping("/{id}")
    public NewsPostResponse getPost(@PathVariable Long id) {
        return newsPostRepository.findById(id)
                .map(NewsPostResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "News post not found"));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NewsPostResponse> createPost(@Valid @RequestBody NewsPostRequest body) {
        NewsPost post = new NewsPost();
        post.setTitle(body.title());
        post.setContent(body.content());
        post.setExcerpt(body.excerpt());
        post.setAuthor(body.author());
        post.setImageUrl(body.imageUrl());
        post = newsPostRepository.save(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(NewsPostResponse.from(post));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePost(@PathVariable Long id) {
        if (!newsPostRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "News post not found");
        }
        newsPostRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
