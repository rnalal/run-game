package com.example.rungame.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/*
* Refresh Token 엔티티
*
* - 로그인 유지 및 Access Token 재발급을 위한 토큰 저장소
* - Refresh Token Rotation 및 강제 로그아웃 처리를 위해 DB에 영속화
*
* 보안 강화를 위해
* - 만료 시간
* - 폐기 여부
* - JWT jti
* 를 함께 관리
* */
@Entity
@Table(name = "refresh_token",
        indexes = {
            /*
            * 사용자 기준 Refresh Token 조회 최적화
            * - 로그아웃, 전체 폐기 처리 용도
            * */
            @Index(name = "idx_refresh_token_user", columnList = "userId"),
            /*
            * 토큰 값 기준 단건 조회
            * - Refresh 요청 시 검증용
            * */
            @Index(name = "idx_refresh_token_token", columnList = "token", unique = true)
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RefreshToken {

    //Refresh Token PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //토큰 소유 사용자 ID
    private Long userId;

    /*
    * Refresh Token 값
    * - 초기 구현은 원문 저장
    * - 추후 해시 저장 방식으로 보안 강화 가능
    * */
    @Column(length = 2000, nullable = false, unique = true)
    private String token;

    /*
    * JWT ID (jti)
    * - 토큰 추적 및 감사에 활용
    * - 토큰 재사용 공격 탐지에 도움
    * */
    @Column(nullable = false)
    private String jti;

    //Refresh Token 만료 시각
    @Column(nullable = false)
    private Instant expiresAt;

    /*
    * 토큰 폐기 여부
    * - true : 더 이상 사용 불가
    * */
    @Column(nullable = false)
    private boolean revoked;

    //토큰 발급 시각
    @Column(nullable = false)
    private Instant createdAt;

    /*
    * Refresh Token 발급 팩토리 메서드
    * - 생성 규칙을 한 곳에서 관리
    * */
    private Instant revokedAt;

    public static RefreshToken issue(Long userId, String token, String jti, Instant expiresAt) {
        return RefreshToken.builder()
                .userId(userId)
                .token(token)
                .jti(jti)
                .expiresAt(expiresAt)
                .revoked(false)
                .createdAt(Instant.now())
                .build();
    }

    /*
    * Refresh Token 폐기 처리
    * - 로그아웃
    * - 재발급 시 기존 토큰 무효화
    * */
    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    //Refresh Token 만료 여부 확인
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
