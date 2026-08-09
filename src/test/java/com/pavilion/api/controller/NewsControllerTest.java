package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.NewsPost;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.NewsPostRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NewsControllerTest extends AbstractIntegrationTest {

    @Autowired
    private NewsPostRepository newsPostRepository;

    private NewsPost createPost(String title) {
        NewsPost post = new NewsPost();
        post.setTitle(title);
        post.setContent("Content");
        post.setAuthor("Admin");
        return newsPostRepository.save(post);
    }

    @Test
    void listIsPublicWithNoAuth() throws Exception {
        createPost("Pool Reopening");
        mockMvc.perform(get("/api/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Pool Reopening"));
    }

    @Test
    void latestIsCappedAtThree() throws Exception {
        for (int i = 0; i < 5; i++) {
            createPost("Post " + i);
        }
        mockMvc.perform(get("/api/news/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void getByIdReturns404ForUnknownPost() throws Exception {
        mockMvc.perform(get("/api/news/999999")).andExpect(status().isNotFound());
    }

    @Test
    void creatingAPostRequiresAdmin() throws Exception {
        mockMvc.perform(post("/api/news")
                        .contentType("application/json")
                        .content("{\"title\":\"Pool Reopening\",\"content\":\"It's open\",\"author\":\"Admin\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanCreateAndDeleteAPost() throws Exception {
        User admin = createUser("admin");

        String response = mockMvc.perform(post("/api/news")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"title\":\"Pool Reopening\",\"content\":\"It's open\",\"author\":\"Admin\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/news/" + id).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }
}
