package com.example.rungame.auth.repository;

import com.example.rungame.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/*
* Refresh Token Repository
*
* - Refresh Token 조회 및 폐기 전용 Repository
* - Refresh Token 재발급, 강제 로그아웃 구현의 핵심 역할
* */
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /*
    * 유효한 Refresh Token 조회
    *
    * - Refresh 요청 시 사용
    * - revoked = false 조건을 통해 이미 무효화된 토큰 재사용 방지
    *
    * @param token Refresh Token 문자열
    * @return 유효한 RefreshToken
    * */
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    /*
    * 특정 사용자의 모든 Refresh Token 폐기
    * - 로그아웃
    * - 권한 변경
    * - 계정 정지 시 사용
    *
    * @param userId 사용자 ID
    * @return 폐기된 토큰 개수
    * */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked=true, rt.revokedAt = CURRENT_TIMESTAMP " +
            "WHERE rt.userId = :userId AND rt.revoked=false")
    int revokeAllByUserId(@Param("userId") Long userId);
}
