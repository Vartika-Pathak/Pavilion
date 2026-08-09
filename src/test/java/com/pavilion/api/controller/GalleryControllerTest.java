package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.GalleryPhoto;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.GalleryPhotoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GalleryControllerTest extends AbstractIntegrationTest {

    @Autowired
    private GalleryPhotoRepository galleryPhotoRepository;

    @Test
    void listIsPublicWithNoAuth() throws Exception {
        GalleryPhoto photo = new GalleryPhoto();
        photo.setImageUrl("https://example.com/photo.jpg");
        photo.setTitle("Pool Party");
        galleryPhotoRepository.save(photo);

        mockMvc.perform(get("/api/gallery"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Pool Party"));
    }

    @Test
    void addingAPhotoRequiresAdmin() throws Exception {
        mockMvc.perform(post("/api/gallery")
                        .contentType("application/json")
                        .content("{\"imageUrl\":\"https://example.com/photo.jpg\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanAddAndDeleteAPhoto() throws Exception {
        User admin = createUser("admin");

        String response = mockMvc.perform(post("/api/gallery")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"imageUrl\":\"https://example.com/photo.jpg\",\"title\":\"Pool Party\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Pool Party"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(delete("/api/gallery/" + id).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }
}
