package com.example.rungame.common.config;

import com.example.rungame.auth.service.RedisTokenService;
import com.example.rungame.common.jwt.JwtAuthenticationFilter;
import com.example.rungame.common.jwt.JwtProvider;
import com.example.rungame.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

//사용자 영역(Spring Security) 보안 설정
@Configuration
@RequiredArgsConstructor
@Order(2)
public class SecurityConfig {

    private final JwtProvider jwtProvider;
    private final RedisTokenService redisTokenService;
    private final UserRepository userRepository;

    //사용자 영역 Security Filter Chain
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        //JWT 인증 필터 생성
        JwtAuthenticationFilter jwtFilter = new JwtAuthenticationFilter(jwtProvider, redisTokenService, userRepository);

        http
                //모든 요청 대상(관리자 체인은 별로도 분리)
                .securityMatcher("/**")
                //CSRF 비활성화 (JWT + Stateless 환경)
                .csrf(csrf -> csrf.disable())
                //세션 사용 안 함 (JWT 기반)
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                //인증/인가 예외 처리
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

                        //정적 리소스 (인증 불필요)
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()

                        //공개 페이지
                        .requestMatchers(
                                "/",
                                "/login",
                                "/signup",
                                "/notices", "/notices/**",
                                "/mypage", "/mypage/**",
                                "/game", "/game/**",
                                "/qna", "/qna/**"
                        ).permitAll()

                        //공개 API
                        .requestMatchers(
                                "/api/users/sign-up",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/public/**"
                        ).permitAll()

                        //API는 로그인 필수
                        .requestMatchers("/api/**").authenticated()

                        //그 외 모든 요청은 로그인 필요
                        .anyRequest().authenticated()
                )

                //JWT 인증 필터 등록
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    //비밀번호 해시 Bean 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
