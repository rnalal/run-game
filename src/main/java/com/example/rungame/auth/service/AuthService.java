package com.example.rungame.auth.service;

import com.example.rungame.auth.dto.AuthDTO;
import com.example.rungame.common.jwt.JwtProvider;
import com.example.rungame.user.domain.User;
import com.example.rungame.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    //로그인 처리
    @Transactional
    public AuthDTO.TokenPairRes login(AuthDTO.LoginReq req) {
        //사용자 조회
        User u = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다."));

        //비밀번호 검증
        if(!encoder.matches(req.getPassword(), u.getPasswordHash())) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        //계정 상태 검증
        if("BANNED".equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("접근이 제한된 계정입니다.");
        }
        if ("DELETED".equalsIgnoreCase(u.getStatus())) {
            throw new IllegalArgumentException("탈퇴한 계정입니다.");
        }

        //Access Token 발급
        String access = jwtProvider.createAccessToken(
                u.getId(), u.getEmail(), u.getNickname(), u.getRole(), u.getTokenVersion()
        );

        //Refresh Token 발급
        String refresh = jwtProvider.createRefreshToken(
                u.getId(), u.getRole(), u.getTokenVersion()
        );

        //Redis 저장
        String refreshJti = jwtProvider.getJti(refresh);

        redisTokenService.saveTokenVersion(u.getId(), u.getTokenVersion());

        redisTokenService.saveRefreshToken(
                u.getId(),
                refreshJti,
                u.getTokenVersion(),
                jwtProvider.getRefreshTtlSeconds()
        );

        return new AuthDTO.TokenPairRes(
                access, refresh, jwtProvider.getAccessTtlSeconds(), jwtProvider.getRefreshTtlSeconds()
        );
    }

    //토큰 재발급
    @Transactional
    public AuthDTO.TokenPairRes refresh(String refreshToken ) {
        //토큰 존재 여부
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new IllegalArgumentException("리프레시 토큰이 없습니다.");
        }
        //JWT 유효성 검증
        if (!jwtProvider.validate(refreshToken)) {
            throw new IllegalArgumentException("유효하지 않거나 만료된 리프레시 토큰입니다.");
        }
        //토큰 타입 검증
        String typ = jwtProvider.getType(refreshToken);
        if (!JwtProvider.TYPE_REFRESH.equals(typ)) {
            throw new IllegalArgumentException("리프레시 토큰 타입이 아닙니다.");
        }

        //Redis 검증
        Long userId = jwtProvider.getUserId(refreshToken);
        int tokenVersionInToken = jwtProvider.getVersion(refreshToken);
        String oldRefreshJti = jwtProvider.getJti(refreshToken);

        if (!redisTokenService.existsRefreshToken(oldRefreshJti)) {
            throw new IllegalArgumentException("이미 폐기되었거나 존재하지 않는 리프레시 토큰입니다.");
        }

        Integer currentVersion = redisTokenService.getTokenVersion(userId);

        if(currentVersion == null){
            User userForVersion = userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

            currentVersion = userForVersion.getTokenVersion();
            redisTokenService.saveTokenVersion(userId, currentVersion);
        }

        if (currentVersion != tokenVersionInToken) {
            throw new IllegalArgumentException("토큰이 무효화되었습니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("비활성화된 계정입니다.");
        }

        redisTokenService.deleteRefreshToken(userId, oldRefreshJti);

        String newAccess = jwtProvider.createAccessToken(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getRole(),
                user.getTokenVersion()
        );

        String newRefresh = jwtProvider.createRefreshToken(
                user.getId(),
                user.getRole(),
                user.getTokenVersion()
        );

        String newRefreshJti = jwtProvider.getJti(newRefresh);

        redisTokenService.saveRefreshToken(
                user.getId(),
                newRefreshJti,
                user.getTokenVersion(),
                jwtProvider.getRefreshTtlSeconds()
        );

        return new AuthDTO.TokenPairRes(
                newAccess, newRefresh, jwtProvider.getAccessTtlSeconds(), jwtProvider.getRefreshTtlSeconds()
        );
    }

    //로그아웃 처리
    @Transactional
    public void logout(String refreshToken, String accessToken) {

        Long userId = null;

        if (accessToken != null && !accessToken.isBlank() && jwtProvider.validate(accessToken)) {
            if (JwtProvider.TYPE_ACCESS.equals(jwtProvider.getType(accessToken))) {
                String accessJti = jwtProvider.getJti(accessToken);
                long remainingSeconds = jwtProvider.getRemainingSeconds(accessToken);

                redisTokenService.addAccessTokenBlacklist(accessJti, remainingSeconds);

                userId = jwtProvider.getUserId(accessToken);
            }
        }

        if (refreshToken != null &&  !refreshToken.isBlank() && jwtProvider.validate(refreshToken)) {
            if (JwtProvider.TYPE_REFRESH.equals(jwtProvider.getType(refreshToken))) {
                Long refreshUserId = jwtProvider.getUserId(refreshToken);
                String refreshJti = jwtProvider.getJti(refreshToken);

                redisTokenService.deleteRefreshToken(refreshUserId, refreshJti);

                if (userId == null) {
                    userId = refreshUserId;
                }
            }
        }
    }

    //전체 로그아웃, 강제 로그아웃
    @Transactional
    public void logoutAll(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        user.incrementTokenVersion();

        redisTokenService.saveTokenVersion(user.getId(), user.getTokenVersion());

        redisTokenService.deleteAllRefreshTokens(user.getId());
    }
}
