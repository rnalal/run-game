package com.example.rungame.auth.controller;

import com.example.rungame.auth.dto.AuthDTO;
import com.example.rungame.auth.service.AuthService;
import com.example.rungame.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.Cookie;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    //로그인 처리
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid AuthDTO.LoginReq req) {
        //로그인 성공 시 토큰 세트 발급
        var tokens = authService.login(req);

        //Access Token 쿠키 설정
        ResponseCookie accessCookie = ResponseCookie.from("accessToken", tokens.getAccessToken())
                .httpOnly(true) //JS 접근 차단
                .secure(false) //운영 HTTPS 환경에서는 true
                .sameSite("Lax") //기본적인 CSRF 완화
                .path("/") //전체 API에 전송
                .maxAge(tokens.getAccessTtlSeconds())
                .build();

        //Refresh Token 쿠키 설정
        //refresh 엔드포인트로만 전송되도록 path 제한
        ResponseCookie refreshCookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/api/auth")
                .maxAge(tokens.getRefreshTtlSeconds())
                .build();

        //쿠키는 Header에 직접 세팅
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, accessCookie.toString());
        headers.add(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body(ApiResponse.ok("login_success"));
    }

    //Access Token 재발급
    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(HttpServletRequest request) {

        //쿠키에서 Refresh Token 추출
        String refreshToken = extractCookie(request, "refreshToken");
        //토큰 재발급
        var tokens = authService.refresh(refreshToken);
        //새 Access Token 쿠키
        ResponseCookie newAccessCookie = ResponseCookie.from("accessToken", tokens.getAccessToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/")
                .maxAge(tokens.getAccessTtlSeconds())
                .build();

        // Refresh Token Rotation -> 새 Refresh Token 발급
        ResponseCookie newRefreshCookie = ResponseCookie.from("refreshToken", tokens.getRefreshToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Strict")
                .path("/api/auth")
                .maxAge(tokens.getRefreshTtlSeconds())
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", newAccessCookie.toString())
                .header("Set-Cookie", newRefreshCookie.toString())
                .body(ApiResponse.ok("token_refreshed"));
    }

    //로그아웃 처리
    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request) {

        String refreshToken = extractCookie(request, "refreshToken");
        String accessToken  = extractCookie(request, "accessToken");

        //서버 측 토큰 무효화 처리
        authService.logout(refreshToken, accessToken);

        //Access Token 쿠키 삭제
        ResponseCookie expiredAccess = ResponseCookie.from("accessToken", "")
                .path("/")
                .maxAge(0)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .build();
        //Refresh Token 쿠키 삭제
        ResponseCookie expiredRefresh = ResponseCookie.from("refreshToken", "")
                .path("/api/auth")
                .maxAge(0)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header("Set-Cookie", expiredAccess.toString())
                .header("Set-Cookie", expiredRefresh.toString())
                .body(ApiResponse.ok("logout_success"));
    }

    //요청 쿠키에서 특정 이름의 쿠키 값 추출
    private String extractCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}