package com.pavilion.api.controller;

import com.pavilion.api.dto.NoticesRulesServicesDtos.NoticeRequest;
import com.pavilion.api.dto.NoticesRulesServicesDtos.NoticeResponse;
import com.pavilion.api.entity.Notice;
import com.pavilion.api.exception.ApiException;
import com.pavilion.api.repository.NoticeRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notices")
@PreAuthorize("hasRole('ADMIN')")
public class NoticeController {

    private final NoticeRepository noticeRepository;

    public NoticeController(NoticeRepository noticeRepository) {
        this.noticeRepository = noticeRepository;
    }

    @GetMapping
    public List<NoticeResponse> listNotices() {
        return noticeRepository.findAllByOrderByPinnedDescCreatedAtDesc().stream().map(NoticeResponse::from).toList();
    }

    @PostMapping
    public ResponseEntity<NoticeResponse> createNotice(@Valid @RequestBody NoticeRequest body) {
        Notice notice = new Notice();
        applyRequest(notice, body);
        notice = noticeRepository.save(notice);
        return ResponseEntity.status(HttpStatus.CREATED).body(NoticeResponse.from(notice));
    }

    @PutMapping("/{id}")
    public NoticeResponse updateNotice(@PathVariable Long id, @Valid @RequestBody NoticeRequest body) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Notice not found"));
        applyRequest(notice, body);
        return NoticeResponse.from(noticeRepository.save(notice));
    }

    private void applyRequest(Notice notice, NoticeRequest body) {
        notice.setTitle(body.title());
        notice.setContent(body.content());
        notice.setCategory(body.category());
        notice.setPriority(body.priority());
        notice.setPinned(body.pinned());
        notice.setExpiresAt(body.expiresAt());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long id) {
        if (!noticeRepository.existsById(id)) {
            throw new ApiException(HttpStatus.NOT_FOUND, "Notice not found");
        }
        noticeRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
