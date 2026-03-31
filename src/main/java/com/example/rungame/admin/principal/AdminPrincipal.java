package com.example.rungame.admin.principal;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/*
* 관리자 인증 principal 객체
*
* - Spring Security에서 인증된 관리자 정보를 표현
* - JWT 인증 후 SecurityContext에 저장되는 사용자 정보
* */
@Getter
public class AdminPrincipal implements UserDetails {
    //관리자(사용자) ID
    private final Long id;
    //로그인 식별자(username 또는 email)
    private final String username;
    /*
    * 관리자 권한 목록
    *
    * - ROLE_ADMIN, ROLE_SUPER_ADMIN 등
    * */
    private final Collection<? extends GrantedAuthority> authorities;

    public AdminPrincipal(
            Long id,
            String username,
            Collection<? extends GrantedAuthority> authorities
    ) {
        this.id = id;
        this.username = username;
        this.authorities = authorities;
    }

    //사용자 권한 반환
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    /*
    * 비밀번호는 사용하지 않음
    *
    * - JWT 기반 인증이므로 null 반환
    * */
    @Override public String getPassword() {
        return null;
    }
    //사용자명 반환
    @Override public String getUsername() {
        return username;
    }
    //계정 만료 여부
    @Override public boolean isAccountNonExpired() {
        return true;
    }
    //계정 잠김 여부
    @Override public boolean isAccountNonLocked() {
        return true;
    }
    //자격 증명 만료 여부
    @Override public boolean isCredentialsNonExpired() {
        return true;
    }
    //계정 활성화 여부
    @Override public boolean isEnabled() {
        return true;
    }
}
