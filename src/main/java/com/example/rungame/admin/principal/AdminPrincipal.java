package com.example.rungame.admin.principal;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

//관리자 인증 principal 객체
@Getter
public class AdminPrincipal implements UserDetails {

    private final Long id;
    //로그인 식별자
    private final String username;
    //관리자 권한 목록
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

    //비밀번호는 사용하지 않음
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
