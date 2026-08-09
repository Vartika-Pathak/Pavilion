package com.pavilion.api.controller;

import com.pavilion.api.AbstractIntegrationTest;
import com.pavilion.api.entity.Member;
import com.pavilion.api.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MemberControllerTest extends AbstractIntegrationTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    void listIsPublicWithNoAuth() throws Exception {
        Member member = new Member();
        member.setName("Alex Sharma");
        member.setFlatNumber("A-101");
        memberRepository.save(member);

        mockMvc.perform(get("/api/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Alex Sharma"));
    }

    @Test
    void getByIdReturns404ForUnknownMember() throws Exception {
        mockMvc.perform(get("/api/members/999999")).andExpect(status().isNotFound());
    }
}
