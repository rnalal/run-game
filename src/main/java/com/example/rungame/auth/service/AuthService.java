package com.example.rungame.auth.service;

import com.example.rungame.auth.domain.RefreshToken;
import com.example.rungame.auth.dto.AuthDTO;
import com.example.rungame.auth.repository.RefreshTokenRepository;
import com.example.rungame.common.jwt.JwtProvider;
import com.example.rungame.user.domain.User;
import com.example.rungame.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/*
* 인증 핵심 서비스
* - 로그인
* - Access/ Refresh Token 발급
* - Refresh Token 재발급
* - 로그아웃 및 토큰 전면 무효화
*
* 보안 관점에서 이 전략들을 결합해 사용
* - JWT (Access/ Refresh 분리)
* - Refresh Token DB 저장
* - Refresh Token 재발급
* - tokenVersion 기반 즉시 무효화
* */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    //사용자 조회 Reporsitory
    private final UserRepository userRepository;
    //Refresh Token 영속화 Repository
    private final RefreshTokenRepository refreshTokenRepository;
    //JWT 생성 및 검증 provider
    private final JwtProvider jwtProvider;
    //비밀번호 검증용 인코더
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    /*
    * 로그인 처리
    *
    * - 이메일, 비밀번호 검증
    * - 계정 상태 확인
    * - Access Token + Refresh Token 발급
    * - Refresh Token DB 저장
    * */
    @Transactional
    public AuthDTO.TokenPairRes login(AuthDTO.LoginReq req) {
        //1)사용자 조회
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        //2)비밀번호 검증
        if(!encoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        //3)계정 상태 검증
        if("BANNED".equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("접근이 제한된 계정입니다.");
        }
        if ("DELETED".equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("탈퇴한 계정입니다.");
        }

        //4) Access Token 발급
        String access = jwtProvider.createAccessToken(
                u.getId(), u.getEmail(), u.getNickname(), u.getRole(), u.getTokenVersion()
        );

        //5) Refresh Token 발급
        String refresh = jwtProvider.createRefreshToken(
                u.getId(), u.getRole(), u.getTokenVersion()
        );

        //6) Refresh Token DB 저장
        Instant refreshExp = Instant.now().plusSeconds(jwtProvider.getRefreshTtlSeconds());
        String jti = jwtProvider.getJti(refresh);

        refreshTokenRepository.save(RefreshToken.issue(u.getId(), refresh, jti, refreshExp));

        return new AuthDTO.TokenPairRes(
                access, refresh, jwtProvider.getAccessTtlSeconds(), jwtProvider.getRefreshTtlSeconds()
        );
    }

    /*
    * 토큰 재발급
    *
    * 검증 단계
    * 1)Refresh Token 존재 여부
    * 2) JWT 서명/만료 검증
    * 3) typ=REFRESH 확인
    * 4) DB 저장 여부 + revoked=false 확인
    * 5) DB 만료 여부 확인
    * 6) tokenVersion 일치 여부 확인
    *
    * 이후 Refresh 재발급 수행
    * */
    @Transactional
    public AuthDTO.TokenPairRes refresh(String refreshToken ) {
        //1) 토큰 존재 여부
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("리프레시 토큰이 없습니다.");
        }
        //2) JWT 유효성 검증
        if (!jwtProvider.validate(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }
        //3) 토큰 타입 검증
        String typ = jwtProvider.getType(refreshToken);
        if (!JwtProvider.TYPE_REFRESH.equals(typ)) {
            throw new IllegalArgumentException("리프레시 토큰 타입이 아닙니다.");
        }

        //4) DB 저장 여부 + 폐기 여부 확인
        RefreshToken saved = refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                .orElseThrow(() -> new IllegalArgumentException("이미 폐기되었거나 존재하지 않는 리프레시 토큰입니다."));

        //5) DB 기준 만료 여부 확인
        if (saved.isExpired()) {
            saved.revoke();
            throw new IllegalArgumentException("만료된 리프레시 토큰입니다.");
        }

        //6) 사용자 및 tokenVersion 검증
        Long userId = jwtProvider.getUserId(refreshToken);
        int ver = jwtProvider.getVersion(refreshToken);

        User user = userRepository.findByIdAndTokenVersion(userId, ver)
                .orElseThrow(() -> new IllegalArgumentException("토큰이 무효화되었거나 사용자 정보를 찾을 수 없습니다."));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

        //7) Refresh 재발급 : 기존 refresh 폐기
        saved.revoke();

        //8) 새 토큰 발급
        String newAccess = jwtProvider.createAccessToken(
                user.getId(), user.getEmail(), user.getNickname(), user.getRole(), user.getTokenVersion()
        );
        String newRefresh = jwtProvider.createRefreshToken(
                user.getId(), user.getRole(), user.getTokenVersion()
        );

        //9) 새 refresh token 저장
        Instant newRefreshExp = Instant.now().plusSeconds(jwtProvider.getRefreshTtlSeconds());
        refreshTokenRepository.save(RefreshToken.issue(user.getId(), newRefresh, jwtProvider.getJti(newRefresh), newRefreshExp));

        return new AuthDTO.TokenPairRes(
                newAccess, newRefresh, jwtProvider.getAccessTtlSeconds(), jwtProvider.getRefreshTtlSeconds()
        );
    }

    /*
    * 로그아웃 처리
    * 1) Refresh Token 폐기
    * 2) 모든 Refresh Token 폐기
    * 3) tokenVersion 증가 -> 기존 Access Token 즉시 무효화
    * */
    @Transactional
    public void logout(String refreshToken, String accessToken) {

        Long userId = null;

        //1) Refresh Token 기반 로그아웃
        if (refreshToken != null && !refreshToken.isBlank() && jwtProvider.validate(refreshToken)) {
            if (JwtProvider.TYPE_REFRESH.equals(jwtProvider.getType(refreshToken))) {
                refreshTokenRepository.findByTokenAndRevokedFalse(refreshToken)
                        .ifPresent(RefreshToken::revoke);
                userId = jwtProvider.getUserId(refreshToken);
            }
        }

        //2) Access Token 기반 보조 식별
        if (userId == null && accessToken != null && !accessToken.isBlank() && jwtProvider.validate(accessToken)) {
            if (JwtProvider.TYPE_ACCESS.equals(jwtProvider.getType(accessToken))) {
                userId = jwtProvider.getUserId(accessToken);
            }
        }

        //3) 사용자 식별 가능 시 전체 무효화
        if (userId != null) {
            //모든 Refresh Token 폐기
            refreshTokenRepository.revokeAllByUserId(userId);

            //tokenVersion 증가 -> 모든 기존 JWT 즉시 무효화
            userRepository.findById(userId).ifPresent(u -> {
                u.incrementTokenVersion();
            });
        }
    }
}
