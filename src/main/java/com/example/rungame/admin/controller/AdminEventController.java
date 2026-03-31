package com.example.rungame.admin.controller;

import com.example.rungame.admin.service.AdminEventRuleService;
import com.example.rungame.admin.service.AdminEventService;
import com.example.rungame.event.dto.EventLogDTO;
import com.example.rungame.event.dto.EventTimelineDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/*
* 관리자 이벤트 관리 컨트롤러
*
* -이벤트 로그 조회 (필터+페이징)
* -특정 세션의 이벤트 타임라인 조회
* -이벤트 데이터 내보내기 (JSON/CSV)
* -이벤트 타입 목록 제공
* -이벤트 룰 조회 및 수정
*
* ADMIN 또는 SUPER_ADMIN 권한을 가진 관리자만 접근 가능
* */
@RestController
@RequestMapping("/rg-admin/events")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminEventController {

    //이벤트 로그 및 통계 관련 비즈니스 로직
    private final AdminEventService adminEventService;
    //이벤트 검증/운영 룰 관리 로직
    private final AdminEventRuleService adminEventRuleService;

    //생성자
    public AdminEventController(AdminEventService adminEventService,
                                AdminEventRuleService adminEventRuleService) {
        this.adminEventService = adminEventService;
        this.adminEventRuleService = adminEventRuleService;
    }

    /*
    * 이벤트 로그 목록 조회 (필터+페이징)
    *
    * -이벤트 타입, 사용자 ID, 세션 ID, 기간 조건으로 필터링 가능
    * -관리자 화면의 이벤트 리스트 테이블 용도
    * */
    @GetMapping("/data")
    public Page<EventLogDTO> list(
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String userId,       //문자열로 받아 안전 변환
            @RequestParam(required = false) String sessionId,    //문자열로 받아 안전 변환
            @RequestParam(required = false) String fromAt,
            @RequestParam(required = false) String toAt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {

        //userId / sessionId 안전 변환
        Long uid = null;
        Long sid = null;

        try {
            if (userId != null && !userId.isBlank())
                uid = Long.parseLong(userId);
        } catch (Exception ignored) {
            //잘못된 숫자 입력 시 필터 조건 무시
        }

        try {
            if (sessionId != null && !sessionId.isBlank())
                sid = Long.parseLong(sessionId);
        } catch (Exception ignored) {}

        //날짜 문자열 -> LocalDateTime 변환
        LocalDateTime from = null;
        LocalDateTime to = null;

        try {
            if (fromAt != null && !fromAt.isBlank())
                from = LocalDateTime.parse(fromAt);
        } catch (Exception ignored) {}

        try {
            if (toAt != null && !toAt.isBlank())
                to = LocalDateTime.parse(toAt);
        } catch (Exception ignored) {}

        //이벤트 로그 조회를 서비스 계층에 위임
        return adminEventService.list(type, uid, sid, from, to, page, size);
    }

    /*
    * 특정 세션의 이벤트 타임라인 조회
    *
    * - 게임 플레이 효율 분석용
    * - 이벤트 발생 순서를 시간 기준으로 정렬하여 제공
    * */
    @GetMapping("/timeline")
    public EventTimelineDTO timeline(@RequestParam Long sessionId) {
        return adminEventService.timeline(sessionId);
    }

    /*
    * 이벤트 데이터 내보내기
    *
    * - format=csv -> CSV 파일 다운로드
    * - format=json -> JSON 응답 반환
    * */
    @GetMapping("/export")
    public ResponseEntity<?> export(
            @RequestParam(defaultValue = "json") String format,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String sessionId,
            @RequestParam(required = false) String fromAt,
            @RequestParam(required = false) String toAt
    ) {
        //공통 필터 파라미터 변환
        Long uid = null;
        Long sid = null;

        try {
            if (userId != null && !userId.isBlank()) uid = Long.parseLong(userId);
        } catch (Exception ignored) {
        }
        try {
            if (sessionId != null && !sessionId.isBlank()) sid = Long.parseLong(sessionId);
        } catch (Exception ignored) {
        }

        LocalDateTime from = null;
        LocalDateTime to = null;

        try {
            if (fromAt != null && !fromAt.isBlank()) from = LocalDateTime.parse(fromAt);
        } catch (Exception ignored) {
        }
        try {
            if (toAt != null && !toAt.isBlank()) to = LocalDateTime.parse(toAt);
        } catch (Exception ignored) {
        }

        //CSV 다운로드
        if ("csv".equalsIgnoreCase(format)) {
            byte[] csv = adminEventService.exportCsv(type, uid, sid, from, to);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=events.csv")
                    .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                    .body(csv);
        }

        //JSON 응답
        //대량 데이터 대응을 위해 페이지 사이즈를 크게 설정
        var page = adminEventService.list(type, uid, sid, from, to, 0, 10_000);
        return ResponseEntity.ok(page.getContent());
    }

    /*
    * 시스템에서 사용 중인 이벤트 타입 목록 반환
    *
    * - 관리자 필터 UI 구성 용도
    * */
    @GetMapping("/types")
    public List<String> types() {
        return List.of(
                com.example.rungame.event.domain.EventType.values()
        ).stream().map(Enum::name).toList();
    }

    /*
    * 현재 이벤트 검증/운영 룰 조회
    *
    * - 최소 점프 간격
    * - 스프린트 최소 지속 시간
    * - 리버스 최대 지속 시간 등
    * */
    @GetMapping("/rules")
    public Map<String, Object> rules() {
        return adminEventRuleService.current();
    }

    /*
    * 이벤트 룰 업데이트
    *
    * - 일부 값만 전달하여 부분 업데이트 가능
    * */
    @PostMapping("/rules")
    public Map<String, Object> updateRules(
            @RequestParam(required = false) Integer jumpMinIntervalMs,
            @RequestParam(required = false) Integer sprintMinDurationMs,
            @RequestParam(required = false) Integer reverseMaxDurationMs,
            @RequestParam(required = false) Boolean enableCheckpointScore
    ) {
        return adminEventRuleService.update(
                jumpMinIntervalMs,
                sprintMinDurationMs,
                reverseMaxDurationMs,
                enableCheckpointScore
        );
    }
}
