package com.example.rungame.auth.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

//Refresh Token 엔티티
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    //Refresh Token 값
    @Column(length = 2000, nullable = false, unique = true)
    private String token;

    //JWT ID (jti)
    @Column(nullable = false)
    private String jti;

    //Refresh Token 만료 시각
    @Column(nullable = false)
    private Instant expiresAt;

    //토큰 폐기 여부
    @Column(nullable = false)
    private boolean revoked;

    //토큰 발급 시각
    @Column(nullable = false)
    private Instant createdAt;

    //Refresh Token 발급 팩토리 메서드
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

    //Refresh Token 폐기 처리
    public void revoke() {
        this.revoked = true;
        this.revokedAt = Instant.now();
    }

    //Refresh Token 만료 여부 확인
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
