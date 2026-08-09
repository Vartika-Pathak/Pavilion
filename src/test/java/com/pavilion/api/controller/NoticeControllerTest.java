package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.Notice;
import com.pavilion.api.entity.User;
import com.pavilion.api.repository.NoticeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class NoticeControllerTest extends AbstractIntegrationTest {

    @Autowired
    private NoticeRepository noticeRepository;

    private Notice createNotice(String title, boolean pinned) {
        Notice notice = new Notice();
        notice.setTitle(title);
        notice.setContent("Content");
        notice.setCategory("general");
        notice.setPriority("normal");
        notice.setPinned(pinned);
        return noticeRepository.save(notice);
    }

    @Test
    void listRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/notices")).andExpect(status().isUnauthorized());
    }

    @Test
    void pinnedNoticesSortFirst() throws Exception {
        User admin = createUser("admin");
        createNotice("Unpinned", false);
        createNotice("Pinned", true);

        mockMvc.perform(get("/api/notices").cookie(sessionCookie(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Pinned"));
    }

    @Test
    void adminCanCreateUpdateAndDeleteANotice() throws Exception {
        User admin = createUser("admin");

        String response = mockMvc.perform(post("/api/notices")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"title\":\"Water Shutdown\",\"content\":\"Water will be off 10am-2pm\",\"category\":\"maintenance\",\"priority\":\"high\",\"pinned\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.category").value("maintenance"))
                .andReturn().getResponse().getContentAsString();
        Number id = com.jayway.jsonpath.JsonPath.read(response, "$.id");

        mockMvc.perform(put("/api/notices/" + id)
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"title\":\"Water Restored\",\"content\":\"Back on\",\"category\":\"maintenance\",\"priority\":\"low\",\"pinned\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Water Restored"));

        mockMvc.perform(delete("/api/notices/" + id).cookie(sessionCookie(admin)))
                .andExpect(status().isNoContent());
    }

    @Test
    void invalidCategoryFails() throws Exception {
        User admin = createUser("admin");
        mockMvc.perform(post("/api/notices")
                        .cookie(sessionCookie(admin))
                        .contentType("application/json")
                        .content("{\"title\":\"Test\",\"content\":\"Test\",\"category\":\"random\",\"priority\":\"low\",\"pinned\":false}"))
                .andExpect(status().isBadRequest());
    }
}
