package com.example.rungame.user.dto;

import com.example.rungame.user.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/*
* 사용자 관련 요청,응답을 한 곳에 모아둔 DTO 모음 클래스
* - 회원가입, 로그인, 로그아웃, 내 정보 조회, 비밀번호/닉네임 변경 등
*   유저 API에서 주고받는 데이터 형태를 정의
* */
public class UserDTO {

    //회원가입
    //요청 DTO
    @Data
    public static class SignUpReq {
        @Email @NotBlank
        private String email;

        @NotBlank @Size(min = 8, max = 64)
        private String password;

        @NotBlank @Size(min = 2, max = 50)
        private String nickname;

    }
    //응답 DTO
    @Getter
    @Builder
    public static class UserRes {
        private Long id;
        private String email;
        private String nickname;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    //로그인
    @Getter
    public static class LoginReq {
        @Email @NotBlank
        private String email;   //클라가 보낸 이메일

        @NotBlank
        private String password; //클라가 보낸 평문 비밀번호
    }
    @Getter
    public static class LoginRes {
        private final String tokenType = "Bearer"; //표준
        private final String accessToken;          //API 호출에 사용하는 토큰
        private final Long userId;
        private final String email;
        private final String nickname;
        private final long expiresIn;              //만료까지 남은 시간(초)

        public LoginRes(String accessToken, Long userId, String email, String nickname, long expiresIn) {
            this.accessToken = accessToken;
            this.userId = userId;
            this.email = email;
            this.nickname = nickname;
            this.expiresIn = expiresIn;
        }
    }

    //로그아웃
    @Getter @Builder
    public static class LogoutRes {
        private Long userId;
        private int newTokenVersion; //증가된 버전(이전 토큰 모두 무효화됨)
        private String message;
    }

    //내 정보 조회+플레이 요약
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MyInfoRes {
        private Long id;
        private String email;
        private String nickname;
        private String status;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        //플레이요약(전체 기준)
        private Long sumScore; //총점수(누적)
        private Long bestScore; //최고 점수

        private Long sumDistance; //총 거리
        private Long bestDistance; //최고 거리

        private Long sumCoins; //총 코인
        private Long bestCoins; //최고 코인

        private Long sessionCount; //플레이 횟수

        private double avgScore; //평균 점수
        private double avgDistance; //평균 거리

        private String lastPlayedAt; //최근 플레이 날짜

        //최근 플레이 5개 요약
        private List<SessionSummary> recentSessions;

        @Data
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class SessionSummary {
            private Long id;
            private String startedAt;
            private String endedAt;
            private int score;
            private int coins;
            private int distance;
        }

        //User 엔티티 -> 기본 프로필 정보만 채우는 헬퍼
        public static MyInfoRes from(User u){
            return MyInfoRes.builder()
                    .id(u.getId())
                    .email(u.getEmail())
                    .nickname(u.getNickname())
                    .status(u.getStatus())
                    .createdAt(u.getCreatedAt())
                    .updatedAt(u.getUpdatedAt())
                    .build();
        }
    }

    //비밀번호 변경 요청
    @Data
    public static class ChangePasswordReq {
        @NotBlank(message = "현재 비밀번호를 입력해주세요.")
        private String currentPassword;

        @NotBlank(message = "새 비밀번호를 입력해주세요.")
        private String newPassword;
    }

    //닉네임 변경 요청
    @Data
    public static class ChangeNicknameReq {
        @NotBlank(message = "새 닉네임을 입력해주세요.")
        private String newNickname;
    }
}
