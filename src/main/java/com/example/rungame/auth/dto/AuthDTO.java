package com.example.rungame.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/*
* 인증 관련 DTO
*
* - 로그인 요청 DTO
* - 토큰 발급 응답 DTO
*
* 컨트롤러와 서비스 간 데이터 전달 전용 객체,
* 인증 로직과 명확히 분리됨
* */
public class AuthDTO {

    /*
    * 로그인 요청 DTO
    * - 이메일/ 비밀번호 입력값 검증 포함
    * */
    @Getter @Setter
    public static class LoginReq {
        @Email @NotBlank
        private String email;

        @NotBlank
        private String password;
    }

    /*
    * 토큰 페어 응답 DTO
    *
    * - Access Token/ Refresh Token 한 쌍
    * - 각 토큰의 TTL(초 단위) 포함
    *
    * 쿠키 만료 시간 설정 및
    * 클라이언트 동기화를 위해 사용됨
    * */
    @Getter
    @AllArgsConstructor
    public static class TokenPairRes {
        private String accessToken;
        private String refreshToken;
        private long accessTtlSeconds;
        private long refreshTtlSeconds;
    }
}
