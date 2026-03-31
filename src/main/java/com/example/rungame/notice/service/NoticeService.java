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

/*
* 공지사항 도메인 서비스
* - 공지 생성,수정,삭제, 관리자용 검색, 공개용 조회/팝업 로직을
*   한 곳에서 처리하는 공지 비즈니스 로직 중심 서비스
*
* - 1)관리자용 공지 관리
*       - 공지 등록, 수정, 삭제, 전체/조건 검색
* - 2)클라이언트 공개용 공지 조회
*       - activeForClient: 현재 시점 기준 활성 공지 리스트
*       - searchPublic: 제목+대상+기간 필터 기반 페이징 조회
*       - getActivePopups: 팝업 공지 필터링 후 DTO로 반환
* - 3)공지 단건 조회
*       - get(id): 존재하지 않으면 NotFoundException 던져서 방어
* */
@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

    /*
    * 공지 전체 목록
    * - 단순히 findAll을 감싼 메서드
    * - 관리자 화면 등에서 기본 리스트가 필요할 때 사용 가능
    * */
    public Page<Notice> list(Pageable pageable) {
        return noticeRepository.findAll(pageable);
    }

    /*
    * 공지 생성
    * - 새 공지 엔티티를 저장하고 반환
    * - createdAt/updatedAt은 엔티티 @PrePersist에서 자동 처리
    * */
    public Notice create(Notice n) {
        return noticeRepository.save(n);
    }

    /*
    * 공지 수정
    * - 기존 공지를 찾아서 제목,내용,팝업 여부,노출 기간,대상만 갱신
    * - 없는 ID면 IllegalArgumentException 발생
    *
    * - 어떤 필드가 수정 가능한지를 서비스 레벨에서 명시적으로 통제
    * */
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

    /*
    * 공지 삭제
    * - 존재 여부 체크 없이 deleteById 호출
    * - 필요하다면 없는 ID 삭제 시 예외 처리도 추가할 수 있음
    * */
    public void delete(Long id) {
        noticeRepository.deleteById(id);
    }

    /*
    * 클라이언트에서 사용하는 현재 활성 공지 조회
    *
    * - now 시각 기준으로 publishAt <now, expireAt> now 인 공지만 조회
    * - title 이 null 이면 빈 문자열로 처리해서 전체 검색
    * */
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

    /*
    * 관리자용 공지 검색
    *
    * - 조건이 하나도 없으면 -> 전체 공지 findAll (sortedPageable)
    * - 하나라도 있으면 -> searchAdmin 쿼리 사용
    * */
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

    /*
    * 공지 단건 조회
    * - 없는 ID면 NotFoundException 발생
    * - PublicNoticeController/ 관리자 API에서 재사용
    * */
    public Notice get(Long id) {
        return noticeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notice not found"));
    }

    /*
    * 현재 활성 팝업 공지 여러 개 조회
    * - isAdmin에 따라 대상 필터링
    *
    * @return
    * - NoticeResponse 리스트
    * - 엔티티를 그대로 보내지 않고 응답 전용 DTO로 감싸서 시간,필드를 정리
    * */
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
