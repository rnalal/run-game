package com.example.rungame.session.controller;

import com.example.rungame.common.jwt.JwtProvider;
import com.example.rungame.event.dto.EventBatchRequest;
import com.example.rungame.event.service.EventIngestService;
import com.example.rungame.session.dto.EndSessionRequest;
import com.example.rungame.session.dto.SessionSummaryResponse;
import com.example.rungame.session.dto.StartSessionRequest;
import com.example.rungame.session.service.SessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class GameSessionController {

    private final SessionService sessionService;
    private final EventIngestService eventIngestService;
    private final JwtProvider jwtProvider;

    //공통: HttpOnly 쿠키에서 accessToken 꺼내기
    private String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie c : request.getCookies()) {
            if ("accessToken".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    //세션 시작
    @PostMapping("/start")
    public ResponseEntity<Long> start(HttpServletRequest request,
                                      @RequestBody StartSessionRequest req) {

        //쿠키에서 토큰 추출
        String token = extractToken(request);

        //토큰검증 - 없거나 만료,위조된 경우 예외
        if (token == null || !jwtProvider.validate(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        //토큰에서 userId 추출
        Long userId = jwtProvider.getUserId(token);

        //세션 시작
        Long sessionId = sessionService.startSession(userId, req);

        //클라이언트는 이 sessionId를 이후 이벤트,종료 요청에 함께 전송
        return ResponseEntity.ok(sessionId);
    }

    //이벤트 배치 수신
    @PostMapping("/{sessionId}/events")
    public ResponseEntity<Integer> ingest(HttpServletRequest request,
                                          @PathVariable Long sessionId,
                                          @RequestBody EventBatchRequest req) {

        //쿠키 토큰 추출 + 검증
        String token = extractToken(request);
        if (token == null || !jwtProvider.validate(token)) {
            throw new IllegalStateException("Invalid or expired token");
        }

        //userId 추출
        Long userId = jwtProvider.getUserId(token);

        //이벤트 배치 처리
        int accepted = eventIngestService.ingest(userId, sessionId, req);

        //저장,반영에 성공한 이벤트 개수 반환
        return ResponseEntity.ok(accepted);
    }

    //세션 종료 + 요약 응답
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<SessionSummaryResponse> end(HttpServletRequest request,
                                                      @PathVariable Long sessionId,
                                                      @RequestBody EndSessionRequest req) {

        //쿠키 토큰 추출 + 검증
        String token = extractToken(request);
        if (token == null || !jwtProvider.validate(token)) {
            throw new IllegalStateException("Invalid or expired token");
        }

        //userId 추출
        Long userId = jwtProvider.getUserId(token);

        //세션 종료 + 요약 정보 생성
        SessionSummaryResponse res = sessionService.endSession(userId, sessionId, req);

        return ResponseEntity.ok(res);
    }

}
