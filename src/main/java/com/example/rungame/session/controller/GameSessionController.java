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

/*
* 게임 세션 관련 REST 컨트롤러
* - 게임 한 판에 해당하는 세션을 시작하고 플레이 도중 발생하는 이벤트들을 서버로 모아서 저장한 뒤
*   마지막 세션을 종료하며 요약 정보를 돌려주는 API 진입점
*
* - POST /api/sessions/start
*   : 게임 세션 시작, 새로운 sessionId 발급
* - POST /api/sessions/{sessionId}/events
*   : 클라이언트에서 모아 보낸 이벤트 배치 처리
* - POST /api/sessions/{sessionId}/end
*   : 세션 종료 처리 + 최종 점수/거리/코인 등 요약 응답
*
* 인증 방식
* - HttpOnly 쿠키에 담긴 accessToken(JWT)을 직접 읽어서
*   - 토큰 유효성 검증
*   - userId 추출
* - 세션 관련 모든 요청은 로그인 사용자 기준으로만 처리됨
*
* 컨트롤러는
* - 1)쿠키에서 토큰 꺼내기
* - 2)토큰 검증 + userId 추출
* - 3)서비스 계층에 위임
* => 비즈니스 로직은 전부 서비스로 밀어내서 컨트롤러는 요청,응답 흐름에만 집중하도록 분리
* */
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class GameSessionController {

    //게임 세션 시작,종료, 요약 계산 등을 담당하는 서비스
    private final SessionService sessionService;
    //클라이언트에서 보내온 이벤트 배치를 검증,저장하는 서비스
    private final EventIngestService eventIngestService;
    //JWT 토큰 검증 및 userId 추출 도구
    private final JwtProvider jwtProvider;

    /*
    * 공통: HttpOnly 쿠키에서 accessToken 꺼내기
    * - 모든 세션 관련 API는 이 메서드를 통해 사용자 인증을 수행
    * */
    private String extractToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;

        for (Cookie c : request.getCookies()) {
            if ("accessToken".equals(c.getName())) {
                return c.getValue();
            }
        }
        return null;
    }

    /*
    * 세션 시작
    * - 클라이언트에서 게임시작 버튼을 눌렀을 때 호출
    * - 1)토큰검증 -> 2)userId 추출 -> 3)세션 생성 -> 4)sessionId 반환
    * */
    @PostMapping("/start")
    public ResponseEntity<Long> start(HttpServletRequest request,
                                      @RequestBody StartSessionRequest req) {

        //1)쿠키에서 토큰 추출
        String token = extractToken(request);

        //2)토큰검증 - 없거나 만료,위조된 경우 예외
        if (token == null || !jwtProvider.validate(token)) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        //3)토큰에서 userId 추출
        Long userId = jwtProvider.getUserId(token);

        //4)세션 시작
        Long sessionId = sessionService.startSession(userId, req);

        //클라이언트는 이 sessionId를 이후 이벤트,종료 요청에 함께 전송
        return ResponseEntity.ok(sessionId);
    }

    /*
    * 이벤트 배치 수신
    * - 일정 주기에 맙춰 클라이언트가 쌓아둔 이벤트 목록을 한 번에 서버로 전송하는 엔드포인트
    * - EventIngestService에서
    *   - 이벤트 정렬
    *   - 유효성 검사
    *   - 세션 점수,거리 업데이트
    *   - game_over 처리
    * */
    @PostMapping("/{sessionId}/events")
    public ResponseEntity<Integer> ingest(HttpServletRequest request,
                                          @PathVariable Long sessionId,
                                          @RequestBody EventBatchRequest req) {

        //1)쿠키 토큰 추출 + 검증
        String token = extractToken(request);
        if (token == null || !jwtProvider.validate(token)) {
            throw new IllegalStateException("Invalid or expired token");
        }

        //2)userId 추출
        Long userId = jwtProvider.getUserId(token);

        //3)이벤트 배치 처리
        int accepted = eventIngestService.ingest(userId, sessionId, req);

        //저장,반영에 성공한 이벤트 개수 반환
        return ResponseEntity.ok(accepted);
    }

    /*
    * 세션 종료 + 요약 응답
    * - 클라이언트가 게임 종료를 서버에 알릴 때 사용
    * - 내부에서
    *   - 세션 상태를 종료 처리
    *   - 최종 점수,거리,코인 등의 요약 정보 계산
    *   - SessionSummaryResponse로 묶어서 반환
    * */
    @PostMapping("/{sessionId}/end")
    public ResponseEntity<SessionSummaryResponse> end(HttpServletRequest request,
                                                      @PathVariable Long sessionId,
                                                      @RequestBody EndSessionRequest req) {

        //1)쿠키 토큰 추출 + 검증
        String token = extractToken(request);
        if (token == null || !jwtProvider.validate(token)) {
            throw new IllegalStateException("Invalid or expired token");
        }

        //2)userId 추출
        Long userId = jwtProvider.getUserId(token);

        //3)세션 종료 + 요약 정보 생성
        SessionSummaryResponse res = sessionService.endSession(userId, sessionId, req);

        return ResponseEntity.ok(res);
    }

}
