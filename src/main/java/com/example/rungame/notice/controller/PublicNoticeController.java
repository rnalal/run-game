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

/*
* 공지사항 공개 API 컨트롤러
*
* - 클라이언트가 보는 공지 목록,상세,팝업 공지를
*   권한과 노출 기간에 맞춰 들려주는 공개용 REST 엔드포인트
*
* - 1)/public/notices/active
*       - 현재 시점 기준으로 노출 가능한 공지 목록 조회
*       - 관리자/일반 사용자 권한에 따라 대상 필터 다르게 적용
* - 2)/public/notices/{id}
*       - 단일 공지 상세 조회
*       - 권한/대상/게시 기간 체크 후 노출
* - 3)/public/notices/page
*       - 제목 필터 + 페이징이 적용된 공지 목록 조회
*       - 관리자/일반 사용자에 따라 대상 필터 다르게 적용
* - 4)/public/notices/popup
*       - 현재 노출 대상인 팝업 공지 목록 조회
*       - 관리자 여부에 따라 ADMIN용 공지 포함 여부 나눔
* */
@RestController
@RequiredArgsConstructor
@RequestMapping("/public/notices")
public class PublicNoticeController {

    //공지 검색,조회,팝업 로직을 담당하는 서비스
    private final NoticeService noticeService;

    /*
    * 현재 활성 공지 목록 조회
    * - URL : GET /public/notices/active
    * - 파라미터 -> title : 제목 부분 검색용
    *
    * - 현재 로그인한 사용자가 ADMIN 계열 권한인지 확인
    * - 대상 리스트 + 제목 필터를 기준으로 현재 시점에 유효한 공지만 반환
    * */
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

    /*
    * 단일 공지 상세 조회
    *
    * NotFoundException
    * 1) 존재하지 않는 공지
    * 2) 일반 사용자가 ADMIN 대상 공지를 요청한 경우
    * 3) 아직 게시 전(publishAt 이후가 아님)인 경우
    * 4) 이미 만료(expireAt 지남)된 공지
    * */
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

    /*
    * 팝업 공지 조회
    * - 여러 개의 팝업 공지를 동시에 반환할 수 있음
    *
    * - isAdmin=true : 관리자 전용 팝업 + 공용 팝업까지 포함해 조회
    * - isAdmin=false : 유저 대상 + 전체 대상 팝업만 조회
    *
    * @return
    * - ResponseEntity.ok(...)로 감싼 팝업 공지 리스트
    * - 실제 내용/구조는 NoticeService.getActiveaPopups(isAdmin)에서 정의
    * */
    @GetMapping("/popup")
    public ResponseEntity<?> getPopup() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null &&
                auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().contains("ADMIN"));

        return ResponseEntity.ok(noticeService.getActivePopups(isAdmin));
    }

}
