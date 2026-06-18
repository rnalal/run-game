package com.example.rungame.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

public class AuthDTO {

    //로그인 요청 DTO
    @Getter @Setter
    public static class LoginReq {
        @Email @NotBlank
        private String email;

        @NotBlank
        private String password;
    }

    //토큰 페어 응답 DTO
    @Getter
    @AllArgsConstructor
    public static class TokenPairRes {
        private String accessToken;
        private String refreshToken;
        private long accessTtlSeconds;
        private long refreshTtlSeconds;
    }
}
