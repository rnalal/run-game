package com.example.rungame.notice.service;

import com.example.rungame.common.exception.NotFoundException;
import com.example.rungame.notice.domain.Notice;
import com.example.rungame.notice.dto.NoticeResponse;
import com.example.rungame.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    //공지 전체 목록
    public Page<Notice> list(Pageable pageable) {
        return noticeRepository.findAll(pageable);
    }

    //공지 생성
    public Notice create(Notice n) {

        System.out.println("publishAt = " + n.getPublishAt());
        System.out.println("expireAt = " + n.getExpireAt());

        return noticeRepository.save(n);
    }

    //공지 수정
    public Notice update(Long id, Notice n) {
        Notice cur = noticeRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Notice not found"));
        cur.setTitle(n.getTitle());
        cur.setContent(n.getContent());
        cur.setPopup(n.isPopup());
        cur.setPublishAt(n.getPublishAt());
        cur.setExpireAt(n.getExpireAt());
        cur.setTarget(n.getTarget());
        return noticeRepository.save(cur);
    }

    //공지 삭제
    public void delete(Long id) {
        noticeRepository.deleteById(id);
    }

    //클라이언트에서 사용하는 현재 활성 공지 조회
    public List<Notice> activeForClient(List<String> allowedTargets, String title) {
        LocalDateTime now = LocalDateTime.now();
        String keyword = (title == null) ? "" : title;

        return noticeRepository.findByTargetInAndPublishAtBeforeAndExpireAtAfterAndTitleContainingIgnoreCase(
                allowedTargets,
                now,
                now,
                keyword
        );
    }

    //관리자용 공지 검색
    public Page<Notice> searchAdmin(String keyword, String target, Boolean popup, Pageable pageable) {
        String k = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        String t = (target == null || target.isBlank() || "ALL".equalsIgnoreCase(target))
                ? null : target.trim();
        Boolean p = popup; //null 허용

        //최신 공지부터 조회하도록 정렬 정보 강제
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        //조건이 하나도 없으면 전체 조회
        if(k == null && t == null && p == null) {
            return noticeRepository.findAll(sortedPageable);
        }
        return noticeRepository.searchAdmin(k, t, p, sortedPageable);
    }

    //공개용 공지 목록 페이징 조회
    public Page<Notice> searchPublic(List<String> targets, String title, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return noticeRepository.searchPublish(targets, title, pageable);
    }

    //공지 단건 조회
    public Notice get(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notice not found"));
    }

    //현재 활성 팝업 공지 여러 개 조회
    public List<NoticeResponse> getActivePopups(boolean isAdmin) {
        List<Notice> list = noticeRepository.findActivePopups();
        List<NoticeResponse> result = new ArrayList<>();

        for(Notice n : list) {
            //관리자라면 ADMIN/ALL 대상 팝업 허용
            if (isAdmin && (n.getTarget().equals("ADMIN") || n.getTarget().equals("ALL"))) {
                result.add(new NoticeResponse(n));
            }
            //일반 사용자라면 USER/ALL 대상 팝업 허용
            if(!isAdmin && (n.getTarget().equals("USER") || n.getTarget().equals("ALL"))) {
                result.add(new NoticeResponse(n));
            }
        }
        return result;
    }
}
