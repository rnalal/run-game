package com.example.rungame.admin.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/*
* 관리자 인증 정보 유틸리티 클래스
*
* - 현재 로그인한 관리자의 ID 조회
* - 관리자 권한 여부 검증
*
* SecurityContext에 저장된 인증 정보를 기반으로 동작함
* */
public class AdminSecurityUtil {

    //유틸리티 클래스이므로 인스턴스 생성 방지
    private AdminSecurityUtil() {}

    /*
    * 현재 인증된 관리자 ID 조회
    *
    * @return : 관리자 ID
    * @throws IllegalStateException : 인증 정보가 없거나 관리자 권한이 아닌 경우
    * */
    public static Long getCurrentAdminId() {
        //현재 SecurityContext에서 Authentication 객체 조회
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        //인증 정보 자체가 없는 경우
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("인증 정보가 없습니다.");
        }

        //Principal이 관리자 ID(Long)가 아닌 경우
        if (!(auth.getPrincipal() instanceof Long adminId)) {
            throw new IllegalStateException("관리자 인증이 아닙니다.");
        }

        //관리자 권한 여부 확인
        boolean isAdmin =
                auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN")) ||
                        auth.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN"));

        //관리자 권한이 없는 경우
        if (!isAdmin) {
            throw new IllegalStateException("관리자 권한이 없습니다.");
        }

        //인증된 관리자 ID 반환
        return adminId;
    }
}
