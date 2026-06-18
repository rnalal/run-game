package com.example.rungame.admin.controller;

import com.example.rungame.admin.service.AdminSessionService;
import com.example.rungame.session.domain.Session;
import com.example.rungame.session.dto.SessionSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/rg-admin/sessions")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
@RequiredArgsConstructor
public class AdminSessionController {

    private final AdminSessionService adminSessionService;

    @GetMapping("/page")
    public String page(){
        return "admin/admin-sessions";
    }

    //실시간 세션 모니터링 조회
    @GetMapping("/monitor")
    public ResponseEntity<Map<String, Object>> monitor(
            @RequestParam(defaultValue = "10") int minutes
    ) {
        return ResponseEntity.ok(adminSessionService.monitor(minutes));
    }

    //세션 목록 조회
    @GetMapping
    @ResponseBody
    public Page<Session> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String fromAt,
            @RequestParam(required = false) String toAt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        LocalDateTime from = null;
        LocalDateTime to = null;

        try {
            if (fromAt != null && !fromAt.isBlank()) {
                from = LocalDateTime.parse(fromAt);
            }
        } catch (Exception ignored) {
        }

        try {
            if (toAt != null && !toAt.isBlank()) {
                to = LocalDateTime.parse(toAt);
            }
        } catch (Exception ignored){
        }

        return adminSessionService.list(
                userId,
                status,
                from,
                to,
                page,
                size
        );
    }

    //세션 강제 종료
    @PostMapping("/{sessionId}/force-end")
    @ResponseBody
    public ResponseEntity<SessionSummaryResponse> forceEnd(@PathVariable Long sessionId) {
        return ResponseEntity.ok(adminSessionService.forceEnd(sessionId));
    }

    //일별 세션 통계 조회
    @GetMapping("/stats/daily")
    @ResponseBody
    public ResponseEntity<List<Map<String, Object>>> dailyStats(
            @RequestParam String from,
            @RequestParam String to
    ) {
        LocalDate f = LocalDate.parse(from);
        LocalDate t = LocalDate.parse(to);

        return ResponseEntity.ok(adminSessionService.dailyStats(f, t));
    }

    //세션 상세 조회
    @GetMapping("/{sessionId}/detail")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> detail(@PathVariable Long sessionId) {
        return ResponseEntity.ok(adminSessionService.detail(sessionId));
    }

    //세션 집계 데이터 재계산
    @PostMapping("/{sessionId}/recalc")
    public ResponseEntity<SessionSummaryResponse> recalc(@PathVariable Long sessionId) {
        return ResponseEntity.ok(adminSessionService.recalc(sessionId));
    }

    //세션 집계 데이터 초기화
    @PostMapping("/{sessionId}/reset")
    public ResponseEntity<SessionSummaryResponse> reset(@PathVariable Long sessionId) {
        return ResponseEntity.ok(adminSessionService.resetAggregates(sessionId));
    }

}
