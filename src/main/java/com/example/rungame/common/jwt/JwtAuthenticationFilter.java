package com.example.rungame.common.jwt;

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

/*
* JWT 인증 필터
*
* - 모든 요청에 대해 한 번만 실행되는 OncePerRequestFilter
* - Access Token(JWT)을 검증하여
*   Spring Security Context에 인증 정보를 세팅
*
* 인증 흐름 요약:
* 1) 공개 경로는 필터 스킵
* 2) Authorization 헤더 또는 Cookie에서 Access Token 추출
* 3) 토큰 유효성 + 타입 검증
* 4) SecurityContext에 Authentication 저장
* */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;

    public JwtAuthenticationFilter(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String requestUri = request.getRequestURI();
        String contextPath = request.getContextPath();
        String path = requestUri.substring(contextPath.length());

        /*
         * 0) 필터 적용 제외 경로
         *
         * - 정적 리소스
         * - 공개 페이지
         * - 공개 API
         *
         * → 인증이 필요 없는 요청은 JWT 처리 없이 바로 통과
         */
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

        /*
         * 1) Authorization 헤더에서 Bearer 토큰 추출
         *
         * - API 요청에서 주로 사용
         */
        String header = request.getHeader("Authorization");
        if(StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            token = jwtProvider.resolveBearer(header);
            System.out.println("헤더 기반 토큰 추출됨");
        }
        /*
         * 2) 헤더에 토큰이 없으면 Cookie에서 accessToken 탐색
         *
         * - 페이지 요청 (브라우저 기반) 대응
         */
        if(token == null && request.getCookies() != null){
            System.out.println("🔎 Authorization 헤더 없음 → 쿠키에서 토큰 검색");

                for(Cookie cookie : request.getCookies()) {
                    if("accessToken".equals(cookie.getName())) {
                        token = cookie.getValue();
                        System.out.println("쿠키 기반 토큰 추출됨");
                        break;
                }
            }
        }

        /*
         * 3) 토큰이 전혀 없는 경우
         *
         * - 익명 사용자로 처리
         * - SecurityContext 비운 뒤 다음 필터로 이동
         */
        if(token == null) {
            System.out.println("토큰 없음 -> ROLE_ANONYMOUS");
            SecurityContextHolder.clearContext();
            filterChain.doFilter(request, response);
            return;
        }

        try {
            //4) 토큰 유효성 검사
            if (jwtProvider.validate(token)) {

                /*
                 * 4-1) 토큰 타입 검사
                 *
                 * - ACCESS 토큰만 인증 처리
                 * - REFRESH 토큰은 인증 컨텍스트에 올리지 않음
                 */
                String typ = jwtProvider.getType(token);
                if (!JwtProvider.TYPE_ACCESS.equals(typ)) {
                    SecurityContextHolder.clearContext();
                    filterChain.doFilter(request, response);
                    return;
                }

                //4-2) JWT Claim 파싱
                Claims claims = Jwts.parser()
                        .verifyWith(jwtProvider.getKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload();

                Long userId = Long.valueOf(claims.getSubject());
                String role = claims.get("role", String.class);

                /*
                 * 4-3) ROLE_ prefix 보장
                 *
                 * - Spring Security 권한 규칙 준수
                 */
                String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                /*
                 * 4-4) Authentication 객체 생성
                 *
                 * - principal : userId
                 * - credentials : null (JWT 기반)
                 * - authorities : ROLE 정보
                 */
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                userId,
                                null,
                                List.of(new SimpleGrantedAuthority(authority))
                        );

                authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                //4-5) SecurityContext에 인증 정보 저장
                SecurityContextHolder.getContext().setAuthentication(authentication);
                System.out.println("JWT 인증 성공: userId=" + userId + " / role=" + authority);
            } else {
                /*
                 * 토큰이 있으나 유효하지 않은 경우
                 *
                 * - 강제 로그아웃 상태로 간주
                 */
                SecurityContextHolder.clearContext();
                System.out.println("토큰 유효성 검사 실패");
            }
        } catch (Exception e) {
            /*
             * JWT 처리 중 예외 발생
             *
             * - 파싱 오류
             * - 서명 오류
             */
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
