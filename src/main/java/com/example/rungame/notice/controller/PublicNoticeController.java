package com.example.rungame.notice.controller;

import com.example.rungame.common.exception.NotFoundException;
import com.example.rungame.notice.domain.Notice;
import com.example.rungame.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/public/notices")
public class PublicNoticeController {

    private final NoticeService noticeService;

    //현재 활성 공지 목록 조회
    @GetMapping("/active")
    public List<Notice> active(@RequestParam(required = false) String title) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null &&
                auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));

        if(isAdmin) {
            //관리자면 ADMIN + ALL + USER 공지 모두 노출
            return noticeService.activeForClient(
                    List.of("ALL", "USER", "ADMIN"),
                    title
            );
        } else {
            //일반 사용자는 USER + ALL 대상 공지만 노출
            return noticeService.activeForClient(
                    List.of("ALL", "USER"),
                    title
            );
        }
    }

    //단일 공지 상세 조회
    @GetMapping("/{id}")
    public Notice detailPublic(@PathVariable Long id) {

        Notice notice = noticeService.get(id);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null &&
                auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));

        //일반 사용자 -> ADMIN 대상 공지는 숨김
        if(!isAdmin && notice.getTarget().equals("ADMIN")) {
            throw new NotFoundException("Notice not found");
        }

        //게시 기간 검증
        LocalDateTime now = LocalDateTime.now();
        if(notice.getPublishAt().isAfter(now) || notice.getExpireAt().isBefore(now)){
            throw new NotFoundException("Notice not found");
        }
        return notice;
    }

    //사용자 목록 페이지 조회
    @GetMapping("/page")
    public Page<Notice> page(
            @RequestParam(required = false) String title,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null &&
                auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));

        List<String> targets = isAdmin ?
                List.of("ALL", "USER", "ADMIN") :
                List.of("ALL", "USER");

        return noticeService.searchPublic(targets, title, page, size);
    }

    //팝업 공지 조회
    @GetMapping("/popup")
    public ResponseEntity<?> getPopup() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null &&
                auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));

        return ResponseEntity.ok(noticeService.getActivePopups(isAdmin));
    }

}
