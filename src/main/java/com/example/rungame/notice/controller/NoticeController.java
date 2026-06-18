package com.example.rungame.notice.controller;

import com.example.rungame.common.exception.NotFoundException;
import com.example.rungame.notice.domain.Notice;
import com.example.rungame.notice.repository.NoticeRepository;
import com.example.rungame.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.crossstore.ChangeSetPersister;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/rg-admin/notices")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class NoticeController {

    private final NoticeService noticeService;
    private final NoticeRepository noticeRepository;

    //관리자용 공지 목록 조회
    @GetMapping
    public Page<Notice> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String target,
            @RequestParam(required = false) Boolean popup,
            Pageable pageable) {

        return noticeService.searchAdmin(keyword, target, popup, pageable);
    }

    //공지 생성
    @PostMapping
    public Notice create(@RequestBody Notice n) {
        return noticeService.create(n);
    }

    //공지 수정
    @PutMapping("/{id}")
    public Notice update(@PathVariable Long id, @RequestBody Notice n) {
        return noticeService.update(id, n);
    }

    //공지 삭제
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        noticeService.delete(id);
    }

    //공지 단건 상세 조회
    @GetMapping("/{id}")
    public Notice detail(@PathVariable Long id) {
        Notice n = noticeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notice not found"));
        return n;
    }

}
