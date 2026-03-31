package com.example.rungame.admin.config;

import com.example.rungame.common.jwt.JwtAuthenticationFilter;
import com.example.rungame.common.jwt.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/*
*  관리자(Admin) 영역 전용 Spring Security 설정
*
* - /admin/** 경로에 대해서만 적용되는 보안 설정
* - JWT 기반 인증 필터 적용
* - 관리자 권한 계층(Role Hierarchy) 정의
*/

@Configuration
@EnableMethodSecurity(jsr250Enabled = true) // @RolesAllowed 등 메서드 단위 권한 체크 활성화
@Order(1) // 여러 SecurityConfig 중 우선순위를 높게 설정 (admin 영역 우선 적용)
public class AdminSecurityConfig {

    // JWT 토큰 생성 및 검증을 담당하는 컴포넌트
    private final JwtProvider jwtProvider;

    //생성자
    public AdminSecurityConfig(JwtProvider jwtProvider) {
        this.jwtProvider = jwtProvider;
    }

    @Bean
    public JwtAuthenticationFilter adminJwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider);
    }

    /*
    * 관리자 권한 계층 설정
    *
    * SUPER_ADMIN > ADMIN > USER
    * 상위 권한은 하위 권한을 모두 포함함
    * */
    @Bean
    public RoleHierarchy roleHierarchy() {
        var h = new RoleHierarchyImpl();
        h.setHierarchy("""
                            ROLE_SUPER_ADMIN > ROLE_ADMIN
                            ROLE_ADMIN > ROLE_USER
                       """);
        return h;
    }

    // 관리자 영역(/admin/**) 전용 Security Filter Chain
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                //이 보안 설정은 /rg-admin/** 요청에만 적용
                .securityMatcher("/rg-admin/**")

                //CSRF 비활성화 (JWT 기반 인증이므로)
                .csrf(csrf -> csrf.disable())

                //인증/인가 예외 처리
                .exceptionHandling(ex -> ex
                        //인증되지 않은 사용자가 접근할 경우 로그인 페이지로 이동
                        .authenticationEntryPoint((req, res, e) -> {
                            res.sendRedirect("/rg-admin/login");
                        })
                        //인증은 되었지만 권한이 부족한 경우도 로그인 페이지로 이동
                        .accessDeniedHandler((req, res, e) -> {
                            res.sendRedirect("/rg-admin/login");
                        })
                )
                //요청 경로별 권한 설정
                .authorizeHttpRequests(auth -> auth
                        //관리자 로그인 페이지는 누구나 접근 가능
                        .requestMatchers("/rg-admin/login").permitAll()

                        //시스템 캐시 관리: ADMIN 이상
                        .requestMatchers("/rg-admin/system/cache/**").hasAnyRole("ADMIN","SUPER_ADMIN")
                        //시스템 백업: SUPER_ADMIN 전용
                        .requestMatchers("/rg-admin/system/backup/**").hasRole("SUPER_ADMIN")
                        //그 외 관리자 페이지
                        .requestMatchers("/rg-admin/**").hasAnyRole("ADMIN","SUPER_ADMIN")

                        // 나머지 요청은 허용
                        .anyRequest().permitAll()
                )
                //JWT 인증 필터를 UsernamePasswordAuthenticationFilter 이전에 등록
                .addFilterBefore(
                        adminJwtAuthenticationFilter(),
                        UsernamePasswordAuthenticationFilter.class
                )
                //CORS 기본 설정 사용
                .cors(cors -> {});
        //SecirutiyFilterChain 반환
        return http.build();
    }
}


