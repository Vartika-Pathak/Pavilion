package com.pavilion.api.controller;

import com.pavilion.api.dto.ContentPagesDtos.MemberResponse;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.MemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// A public directory/profile listing, distinct from the login "users" table. Read-only: no
// admin write API existed in the Node version either.
@RestController
@RequestMapping("/api/members")
public class MemberController {

    private final MemberRepository memberRepository;

    public MemberController(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @GetMapping
    public List<MemberResponse> listMembers() {
        return memberRepository.findAllByOrderByJoinedAtAsc().stream().map(MemberResponse::from).toList();
    }

    @GetMapping("/{id}")
    public MemberResponse getMember(@PathVariable Long id) {
        return memberRepository.findById(id)
                .map(MemberResponse::from)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Member not found"));
    }
}
