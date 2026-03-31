package com.example.rungame.common.config;

import com.example.rungame.common.jwt.JwtAuthenticationFilter;
import com.example.rungame.common.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/*
* 사용자 영역(Spring Security) 보안 설정
* - JWT 기반 인증
* - 세션 미사용
* - API/ 페이지 요청에 따라 예외 응답 분리
*
* 관리자 영역(AdminSecurityConfig)과 분리되어 있고
* @Order(2)로 우선순위를 낮게 설정
* */
@Configuration
@RequiredArgsConstructor
@Order(2)
public class SecurityConfig {

    private final JwtProvider jwtProvider;

    //사용자 영역 Security Filter Chain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        //JWT 인증 필터 생성
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtProvider);

        http
                //모든 요청 대상(관리자 체인은 별로도 분리)
                .securityMatcher("/**")
                //CSRF 비활성화 (JWT + Stateless 환경)
                .csrf(csrf -> csrf.disable())
                //세션 사용 안 함 (JWT 기반)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                /*
                * 인증/인가 예외 처리
                * - API 요쳥 -> JSON 응답
                * - 페이지 요청 -> 로그인 페이지 리다이렉트
                * */
                .exceptionHandling(eh -> eh
                        //인증되지 않은 경우
                        .authenticationEntryPoint((req, res, ex) -> {
                            String uri = req.getRequestURI();
                            if (uri.startsWith("/api/")) {
                                //API 요청 : 401 JSON
                                res.setStatus(401);
                                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                res.getWriter().write("{\"success\":false,\"message\":\"로그인이 필요합니다.\"}");
                            } else {
                                //페이지 요청: 로그인 페이지 이동
                                res.sendRedirect("/login?error=need_login");
                            }
                        })
                        //권한이 없는 경우
                        .accessDeniedHandler((req, res, ex) -> {
                            String uri = req.getRequestURI();
                            if (uri.startsWith("/api/")) {
                                //API 요청: 403 JSON
                                res.setStatus(403);
                                res.setContentType(MediaType.APPLICATION_JSON_VALUE);
                                res.getWriter().write("{\"success\":false,\"message\":\"권한이 없습니다.\"}");
                            } else {
                                //페이지 요청: 로그인 페이지 이동
                                res.sendRedirect("/login?error=no_permission");
                            }
                        })
                )
                //요청별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth

                        // 1) 정적 리소스 (인증 불필요)
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        // 2) 공개 페이지
                        .requestMatchers(
                                "/",
                                "/login",
                                "/signup",
                                "/notices", "/notices/**",
                                "/mypage", "/mypage/**",
                                "/game", "/game/**",
                                "/qna", "/qna/**"
                        ).permitAll()

                        // 3) 공개 API
                        .requestMatchers(
                                "/api/users/sign-up",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/public/**"
                        ).permitAll()

                        // 4) API는 로그인 필수
                        .requestMatchers("/api/**").authenticated()

                        // 5) 그 외 모든 요청은 로그인 필요
                        .anyRequest().authenticated()
                )

                /*
                * JWT 인증 필터 등록
                *
                * - UsernamePasswordAuthenticationFilter 이전에 실행
                * - Authorization 헤더/ Cookie 에서 JWT 추출 및 검증
                * */
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
