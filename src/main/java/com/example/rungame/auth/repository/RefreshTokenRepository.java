package com.example.rungame.auth.repository;

import com.example.rungame.auth.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    //유효한 Refresh Token 조회
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    //특정 사용자의 모든 Refresh Token 폐기
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked=true, rt.revokedAt = CURRENT_TIMESTAMP " +
            "WHERE rt.userId = :userId AND rt.revoked=false")
    int revokeAllByUserId(@Param("userId") Long userId);
}
