package com.pavilion.api.repository;

import com.pavilion.api.entity.NewsPost;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface NewsPostRepository extends JpaRepository<NewsPost, Long> {
    List<NewsPost> findAllByOrderByPublishedAtDesc();

    List<NewsPost> findAllByOrderByPublishedAtDesc(Pageable pageable);
}
