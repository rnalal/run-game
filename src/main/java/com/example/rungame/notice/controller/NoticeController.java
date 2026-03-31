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

/*
* 관리자용 공지사항 관리 REST 컨트롤러
* - 관리자가 공지사항을 검색,조회,등록,수정,삭제 할 수 있게 해주는 API
*
* - 1)공지 목록 조회
*       - 키워드/대상/팝업 여부 조건으로 검색
*       - pageable을 사용한 페이징 지원
* - 2)공지 생성/수정/삭제
*       - Notice 엔티티를 직접 RequestBody로 받아 생성/수정
*       - 특정 공지 삭제
* - 3)공지 단건 상세 조회
*       - 관리자 화면에서 공지 내용을 열람/수정하기 위한 용도
*
* @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
* - 관리자/최고관리자만 접근 가능
* - 일반 유저는 이 컨트롤러에 접근할 수 없음
* */
@RestController
@RequiredArgsConstructor
@RequestMapping("/rg-admin/notices")
@PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
public class NoticeController {

    //공지 검색,등록,수정,삭제 등 핵심 도메인 로직을 담당하는 서비스
    private final NoticeService noticeService;
    /*
    * 단건 공지 조회 등 간단한 DB 접근에 사용하는 레포지토리
    * (상세 조회 정도만 직접 사용, 나머지는 서비스에 위임)
    * */
    private final NoticeRepository noticeRepository;

    /*
    * 관리자용 공지 목록 조회
    *
    * @return
    *   - Page<Notice> : 조건에 맞는 공지 목록
    * */
    @GetMapping
    public Page<Notice> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String target,
            @RequestParam(required = false) Boolean popup,
            Pageable pageable) {

        return noticeService.searchAdmin(keyword, target, popup, pageable);
    }

    /*
    * 공지 생성
    * - RequestBody로 전달된 Notice 데이터를 바탕으로 새 공지를 등록
    * - 작성자 정보/작성 시각 등은 서비스/엔티티 레벨에서 처리 가능
    *
    * @param n : 생성할 공지 정보
    * @return : 생성 완료된 공지 엔티티
    * */
    @PostMapping
    public Notice create(@RequestBody Notice n) {
        return noticeService.create(n);
    }

    /*
    * 공지 수정
    * - path의 {id}로 수정 대상 공지를 지정
    * - RequestBody의 내용으로 해당 공지를 갱신
    *
    * @param id : 수정할 공지 ID
    * @param n : 수정 내용이 담긴 Notice
    * @return : 수정 완료된 공지 엔티티
    * */
    @PutMapping("/{id}")
    public Notice update(@PathVariable Long id, @RequestBody Notice n) {
        return noticeService.update(id, n);
    }

    /*
    * 공지 삭제
    * - path의 {id}에 해당하는 공지를 삭제
    * - 성공 시 별도 바디 없이 200 또는 204 응답
    *
    * @param id : 삭제할 공지 ID
    * */
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        noticeService.delete(id);
    }

    /*
    * 공지 단건 상세 조회
    * - 관리자 화면에서 공지 상세 내용을 수정/확인할 때 사용
    * - 존재하지 않는 ID 요청 시 NotFoundException 발생
    *
    * @param id : 조회할 공지 ID
    * @return : 해당 공지 엔티티
    * */
    @GetMapping("/{id}")
    public Notice detail(@PathVariable Long id) {
        Notice n = noticeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notice not found"));
        return n;
    }

}
