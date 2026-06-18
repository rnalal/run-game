package com.example.rungame.common.jwt;

import com.example.rungame.auth.service.RedisTokenService;
import com.example.rungame.user.domain.User;
import com.example.rungame.user.repository.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException; import java.util.List;

//JWT 인증 필터
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtProvider jwtProvider,
            RedisTokenService redisTokenService,
            UserRepository userRepository
    ) {
        this.jwtProvider = jwtProvider;
        this.redisTokenService = redisTokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri.substring(contextPath.length());

        //필터 적용 제외 경로
        if (
            //정적 리소스
                path.startsWith("/js/") ||
                        path.startsWith("/css/") ||
                        path.startsWith("/images/") ||
                        path.equals("/favicon.ico") ||

                        //공개 페이지
                        path.equals("/") ||
                        path.equals("/login") ||
                        path.equals("/signup") ||
                        path.startsWith("/notices") ||

                        //공개 API
                        path.startsWith("/api/auth/") ||
                        path.startsWith("/api/users/sign-up") ||
                        path.startsWith("/public/")
        ) {
            filterChain.doFilter(request, response);
            return;
        }

        //디버깅 로그
        System.out.println("🔎 Authorization 헤더: " + request.getHeader("Authorization"));
        System.out.println("🧩 JwtAuthenticationFilter 호출됨: " + request.getRequestURI());

        String token = null;

        //Authorization 헤더에서 Bearer 토큰 추출
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = jwtProvider.resolveBearer(header);
            System.out.println("헤더 기반 토큰 추출됨");
        }

        //헤더에 토큰이 없으면 Cookie에서 accessToken 탐색
        if (token == null && request.getCookies() != null) {
            System.out.println("🔎 Authorization 헤더 없음 → 쿠키에서 토큰 검색");

            for (Cookie cookie : request.getCookies()) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                    System.out.println("쿠키 기반 토큰 추출됨");
                    break;
                }
            }
        }

        //토큰이 전혀 없는 경우
        if (token == null) {
            System.out.println("토큰 없음 -> ROLE_ANONYMOUS");
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            //토큰 유효성 검사
            if (jwtProvider.validate(token)) {

                //토큰 타입 검사
                String typ = jwtProvider.getType(token);
                if (!JwtProvider.TYPE_ACCESS.equals(typ)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                //JWT Claim 파싱
                Claims claims = Jwts.parser()
                        .verifyWith(jwtProvider.getKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                Long userId = Long.valueOf(claims.getSubject());
                String role = claims.get("role", String.class);

                String accessJti = claims.getId();

                if (redisTokenService.isAccessTokenBlacklisted(accessJti)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                int tokenVersionInToken = jwtProvider.getVersion(token);

                Integer currentTokenVersion = redisTokenService.getTokenVersion(userId);

                if (currentTokenVersion == null) {
                    User user = userRepository.findById(userId)
                            .orElse(null);

                    if (user == null || !"ACTIVE".equalsIgnoreCase(user.getStatus())) {
                        SecurityContextHolder.clearContext();
                        filterChain.doFilter(request, response);
                        return;
                    }

                    currentTokenVersion = user.getTokenVersion();
                    redisTokenService.saveTokenVersion(userId, currentTokenVersion);
                }

                if (currentTokenVersion != tokenVersionInToken) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                //ROLE_ prefix 보장
                String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                //Authentication 객체 생성
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority(authority))
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                //SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("JWT 인증 성공: userId=" + userId + " / role=" + authority);
            } else {
                //토큰이 있으나 유효하지 않은 경우
                SecurityContextHolder.clearContext();
                System.out.println("토큰 유효성 검사 실패");
            }
        } catch (Exception e) {
            //JWT 처리 중 예외 발생
            SecurityContextHolder.clearContext();
            System.out.println("JWT 처리 중 오류 발생: " + e.getMessage());
        }

        //다음 필터로 요청 전달
        filterChain.doFilter(request, response);

        //최종 인증 상태 로그
        var auth = SecurityContextHolder.getContext().getAuthentication();
        System.out.println("➡️ 최종 SecurityContext auth = " + auth);
    }
}
