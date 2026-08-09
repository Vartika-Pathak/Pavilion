package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.AppEvent;
import com.pavilion.api.entity.Member;
import com.pavilion.api.entity.NewsPost;
import com.pavilion.api.repository.AppEventRepository;
import com.pavilion.api.repository.MemberRepository;
import com.pavilion.api.repository.NewsPostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class StatsControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;
    @Autowired
    private AppEventRepository appEventRepository;
    @Autowired
    private NewsPostRepository newsPostRepository;

    @Test
    void statsAreCountedAcrossResourcesWithNoAuthRequired() throws Exception {
        Member member = new Member();
        member.setName("Alex");
        member.setFlatNumber("A-101");
        memberRepository.save(member);

        for (int i = 0; i < 7; i++) {
            AppEvent event = new AppEvent();
            event.setTitle("Event " + i);
            event.setEventDate(Instant.now().plus(i + 1, ChronoUnit.DAYS));
            event.setLocation("Clubhouse");
            appEventRepository.save(event);
        }
        AppEvent pastEvent = new AppEvent();
        pastEvent.setTitle("Past Event");
        pastEvent.setEventDate(Instant.now().minus(1, ChronoUnit.DAYS));
        pastEvent.setLocation("Clubhouse");
        appEventRepository.save(pastEvent);

        NewsPost post = new NewsPost();
        post.setTitle("News");
        post.setContent("Content");
        post.setAuthor("Admin");
        newsPostRepository.save(post);

        mockMvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalMembers").value(1))
                .andExpect(jsonPath("$.upcomingEventsCount").value(7))
                .andExpect(jsonPath("$.totalNewsPosts").value(1))
                .andExpect(jsonPath("$.totalGalleryPhotos").value(0));
    }
}
