package com.example.rungame.admin.controller;

import com.example.rungame.event.domain.SessionEvent;
import com.example.rungame.event.repository.SessionEventRepository;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.dto.SessionSummaryResponse;
import com.example.rungame.session.repository.SessionRepository;
import com.example.rungame.session.service.SessionMonitorService;
import com.example.rungame.session.service.SessionRecalculationService;
import com.example.rungame.session.service.SessionService;
import com.example.rungame.session.service.SessionStatsService;
import lombok.RequiredArgsConstructor;
import org.apache.coyote.Response;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/*
* 관리자 세션 관리 컨트롤러
*
* - 세션 목록 / 상세 조회
* - 실시간 세션 모니터링
* - 세션 강제 종료
* - 세션 통계 조회
* - 세션 데이터 재계산 / 초기화
*
* 운영 및 장애 대응을 위한 관리자 전용 기능을 담당
* ADMIN 또는 SUPER_ADMIN 권한 필요
* */
@Controller
@RequestMapping("/rg-admin/sessions")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminSessionController {

    //실시간 세션 상태 모니터링 서비스
    private final SessionMonitorService sessionMonitorService;
    //세션 통계 집계 서비스
    private final SessionStatsService sessionStatsService;
    //세션 조회용 Repository
    private final SessionRepository sessionRepository;
    //세션 이벤트 조회용 Repository
    private final SessionEventRepository sessionEventRepository;
    //세션 비즈니스 로직 (강제 종료 등)
    private final SessionService sessionService;
    //세션 집계 데이터 재계산 / 초기화 서비스
    private final SessionRecalculationService sessionRecalculationService;

    //관리자 세션 관리 페이지(view)
    @GetMapping("/page")
    public String page(){
        return "admin/admin-sessions";
    }

    /*
    * 실시간 세션 모니터링 스냅샷 조회
    *
    * @Param minutes : 최근 N분 기준
    * @return : 활성 세션, 이벤트 발생 현황 등 모니터링 정보
    * */
    @GetMapping("/monitor")
    public ResponseEntity<Map<String, Object>> monitor(
            @RequestParam(defaultValue = "10") int minutes
    ) {
        return ResponseEntity.ok(sessionMonitorService.snapshot(minutes));
    }

    /*
    * 세션 목록 조회 (스켈레톤)
    *
    * - 향후 사용자 / 상태 필터링 확장 예정
    * */
    @GetMapping
    public Map<String, Object> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status
    ) {
        return Map.of("message", "sessions list skeleton", "userId", userId, "status", status);
    }

    /*
    * 세션 강제 종료 (관리자)
    *
    * - 비정상 세션 종료
    * - 운영 중 장애 대응 용도
    * */
    @PostMapping("/{sessionId}/force-end")
    public ResponseEntity<SessionSummaryResponse> forceEnd(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionService.adminForceEnd(sessionId));
    }

    /*
    * 일별 세션 통계 조회
    *
    * @param from : 시작 날짜 (yyyy-MM-dd)
    * @param to : 종료 날짜 (yyyy-MM-dd)
    * */
    @GetMapping("/stats/daily")
    public ResponseEntity<List<Map<String, Object>>> dailyStats(
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDate f = LocalDate.parse(from);
        LocalDate t = LocalDate.parse(to);
        return ResponseEntity.ok(sessionStatsService.dailyStats(f, t));
    }

    /*
    * 세션 상세 조회
    *
    * - 세션 기본 정보
    * - 해당 세션에 발생한 이벤트 전체 목록
    * */
    @GetMapping("/{sessionId}/detail")
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long sessionId) {
        //세션 조회
        Session s = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("session not found"));
        //세션 이벤트 조회 (발생 순서 기준)
        List<SessionEvent> events = sessionEventRepository.findBySessionIdOrderBySeqAsc(sessionId);

        return ResponseEntity.ok(Map.of(
           "session", s,
           "events", events
        ));
    }

    /*
    * 세션 집계 데이터 재계산
    *
    * - 이벤트 로그 기준으로 점수/통계 재계산
    * - 집계 오류 복구 용도
    * */
    @PostMapping("/{sessionId}/recalc")
    public ResponseEntity<SessionSummaryResponse> recalc(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionRecalculationService.recalc(sessionId));
    }

    /*
    * 세션 집계 데이터 초기화
    *
    * - 점수, 통계값 리셋
    * - 테스트/ 운영 보정 용도
    * */
    @PostMapping("/{sessionId}/reset")
    public ResponseEntity<SessionSummaryResponse> reset(@PathVariable Long sessionId) {
        return ResponseEntity.ok(sessionRecalculationService.resetAggregates(sessionId));
    }

}
