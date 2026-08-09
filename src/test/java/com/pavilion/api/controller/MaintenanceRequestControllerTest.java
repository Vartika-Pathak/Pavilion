package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MaintenanceRequestControllerTest extends AbstractIntegrationTest {

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/maintenance")).andExpect(status().isUnauthorized());
    }

    @Test
    void residentCanCreateARequestWithoutPhotos() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(multipart("/api/maintenance")
                        .cookie(sessionCookie(resident))
                        .param("category", "plumbing")
                        .param("description", "Leaking tap in the kitchen"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("plumbing"))
                .andExpect(jsonPath("$.photoUrls.length()").value(0));
    }

    @Test
    void residentCanCreateARequestWithAPhoto() throws Exception {
        User resident = createUser("resident");
        MockMultipartFile photo = new MockMultipartFile("photos", "leak.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        mockMvc.perform(multipart("/api/maintenance")
                        .file(photo)
                        .cookie(sessionCookie(resident))
                        .param("category", "plumbing")
                        .param("description", "Leaking tap in the kitchen"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.photoUrls.length()").value(1))
                .andExpect(jsonPath("$.photoUrls[0]").value(org.hamcrest.Matchers.startsWith("/api/uploads/")));
    }

    // Guards against a real regression that slipped past every other test here: those only check
    // the shape of the URL, never that the bytes behind it are actually readable back out of the
    // database (SQLite's JDBC driver doesn't implement getBlob(), so a @Lob-mapped byte[] field
    // saves fine but throws on every read — see UploadedFile's JdbcTypeCode override).
    @Test
    void anUploadedPhotoCanBeFetchedBackWithItsOriginalBytes() throws Exception {
        User resident = createUser("resident");
        byte[] original = "fake-image-bytes".getBytes();
        MockMultipartFile photo = new MockMultipartFile("photos", "leak.jpg", "image/jpeg", original);

        String response = mockMvc.perform(multipart("/api/maintenance")
                        .file(photo)
                        .cookie(sessionCookie(resident))
                        .param("category", "plumbing")
                        .param("description", "Leaking tap"))
                .andReturn().getResponse().getContentAsString();
        String url = com.jayway.jsonpath.JsonPath.read(response, "$.photoUrls[0]");

        mockMvc.perform(get(url).cookie(sessionCookie(resident)))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().contentType("image/jpeg"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().bytes(original));
    }

    @Test
    void fetchingAnUploadedPhotoRequiresAuthentication() throws Exception {
        User resident = createUser("resident");
        MockMultipartFile photo = new MockMultipartFile("photos", "leak.jpg", "image/jpeg", "fake-image-bytes".getBytes());

        String response = mockMvc.perform(multipart("/api/maintenance")
                        .file(photo)
                        .cookie(sessionCookie(resident))
                        .param("category", "plumbing")
                        .param("description", "Leaking tap"))
                .andReturn().getResponse().getContentAsString();
        String url = com.jayway.jsonpath.JsonPath.read(response, "$.photoUrls[0]");

        mockMvc.perform(get(url)).andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsANonImageFile() throws Exception {
        User resident = createUser("resident");
        MockMultipartFile file = new MockMultipartFile("photos", "notes.txt", "text/plain", "hello".getBytes());

        mockMvc.perform(multipart("/api/maintenance")
                        .file(file)
                        .cookie(sessionCookie(resident))
                        .param("category", "plumbing")
                        .param("description", "Leaking tap"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void invalidCategoryFails() throws Exception {
        User resident = createUser("resident");

        mockMvc.perform(multipart("/api/maintenance")
                        .cookie(sessionCookie(resident))
                        .param("category", "cosmetic")
                        .param("description", "Wall needs paint"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void residentOnlySeesTheirOwnRequests() throws Exception {
        User residentA = createUser("resident");
        User residentB = createUser("resident");

        mockMvc.perform(multipart("/api/maintenance")
                .cookie(sessionCookie(residentA))
                .param("category", "plumbing")
                .param("description", "Leak"));
        mockMvc.perform(multipart("/api/maintenance")
                .cookie(sessionCookie(residentB))
                .param("category", "electrical")
                .param("description", "Flicker"));

        mockMvc.perform(get("/api/maintenance").cookie(sessionCookie(residentA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].category").value("plumbing"));
    }

    @Test
    void residentCannotUpdateStatus() throws Exception {
        User resident = createUser("resident");
        String response = mockMvc.perform(multipart("/api/maintenance")
                        .cookie(sessionCookie(resident))
                        .param("category", "plumbing")
                        .param("description", "Leak"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(post("/api/maintenance/" + id + "/status")
                        .cookie(sessionCookie(resident))
                        .contentType("application/json")
                        .content("{\"status\":\"resolved\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void guardCanUpdateStatus() throws Exception {
        User resident = createUser("resident");
        User guard = createUser("guard");
        String response = mockMvc.perform(multipart("/api/maintenance")
                        .cookie(sessionCookie(resident))
                        .param("category", "plumbing")
                        .param("description", "Leak"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(post("/api/maintenance/" + id + "/status")
                        .cookie(sessionCookie(guard))
                        .contentType("application/json")
                        .content("{\"status\":\"in_progress\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("in_progress"));
    }
}
