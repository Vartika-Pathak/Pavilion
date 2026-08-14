package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.GalleryPhoto;
import com.pavilion.api.entity.UploadedFile;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.GalleryPhotoRepository;
import com.pavilion.api.repository.UploadedFileRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GalleryControllerTest extends AbstractIntegrationTest {

    @Autowired
    private GalleryPhotoRepository galleryPhotoRepository;

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

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
    void addingAPhotoRequiresAuthentication() throws Exception {
        MockMultipartFile photo = new MockMultipartFile("photo", "party.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        mockMvc.perform(multipart("/api/gallery").file(photo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void residentCannotAddAPhoto() throws Exception {
        User resident = createUser("resident");
        MockMultipartFile photo = new MockMultipartFile("photo", "party.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        mockMvc.perform(multipart("/api/gallery").file(photo).cookie(sessionCookie(resident)))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanUploadAndDeleteAPhoto() throws Exception {
        User admin = createUser("admin");
        MockMultipartFile photo = new MockMultipartFile("photo", "party.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        String response = mockMvc.perform(multipart("/api/gallery")
                        .file(photo)
                        .param("title", "Pool Party")
                        .cookie(sessionCookie(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Pool Party"))
                .andExpect(jsonPath("$.uploadedBy").value(admin.getName()))
                .andExpect(jsonPath("$.imageUrl", startsWith("/api/uploads?id=")))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");
        String imageUrl = com.jayway.jsonpath.JsonPath.read(response, "$.imageUrl");
        long uploadedFileId = Long.parseLong(imageUrl.substring(imageUrl.lastIndexOf('=') + 1));
        assertThat(uploadedFileRepository.existsById(uploadedFileId)).isTrue();

        mockMvc.perform(delete("/api/gallery/" + id).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());

        // Deleting the gallery entry also cleans up the underlying stored file.
        assertThat(uploadedFileRepository.existsById(uploadedFileId)).isFalse();
    }

    @Test
    void deletingASeededPhotoWithAnExternalUrlDoesNotTouchUnrelatedUploadedFiles() throws Exception {
        User admin = createUser("admin");

        // An UploadedFile whose id happens to collide with a number embedded in the external URL's
        // query string below — proves the external-URL branch is never handed to deleteByUrls.
        UploadedFile unrelated = new UploadedFile();
        unrelated.setFilename("unrelated.jpg");
        unrelated.setContentType("image/jpeg");
        unrelated.setSize(3);
        unrelated.setData(new byte[] {1, 2, 3});
        unrelated = uploadedFileRepository.save(unrelated);

        GalleryPhoto seeded = new GalleryPhoto();
        seeded.setImageUrl("https://images.unsplash.com/photo-123?w=1600&q=" + unrelated.getId());
        seeded.setTitle("Seeded");
        seeded = galleryPhotoRepository.save(seeded);

        mockMvc.perform(delete("/api/gallery/" + seeded.getId()).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());

        assertThat(uploadedFileRepository.existsById(unrelated.getId())).isTrue();
    }
}
