package com.example.rungame.admin.spec;

import com.example.rungame.user.domain.User;
import org.springframework.data.jpa.domain.Specification;

// 관리자 사용자 검색용 JPA Specification 모음
public final class UserAdminSpecs {

    //유틸리티 클래스이므로 인스턴스 생성 방지
    private UserAdminSpecs(){ }

    //닉네임 부분 검색 (대소문자 무시)
    public static Specification<User> nicknameContains(String nickname){
        return (root, query, cb) -> {
            //검색 조건이 없으면 항상 true
            if (nickname == null || nickname.isBlank()) return cb.conjunction();
            //lower(nickname) LIKE %keyword%
            return cb.like(cb.lower(root.get("nickname")), "%" + nickname.toLowerCase() + "%");
        };

    }

    //이메일 부분 검색 (대소문자 무시)
    public static Specification<User> emailContains(String email){
        return (root, query, cb) -> {
            if(email == null || email.isBlank()) return cb.conjunction();
            return cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase() + "%");
        };
    }

    //사용자 상태 필터
    public static Specification<User> statusEquals(String status) {
        return (root, query, cb) -> {
            if (status == null || status.isBlank()) {
                //조건 미적용
                return null;
            }
            return cb.equal(root.get("status"), status.trim().toUpperCase());
        };
    }

    //사용자 권한 필터
    public static Specification<User> roleEquals(String role){
        return (root, query, cb) -> {
            if(role == null || role.isBlank()) return null;
            return cb.equal(root.get("role"), role.trim().toUpperCase());
        };
    }

    //사용장 ID 정확 일치 검색
    public static Specification<User> idEquals(Long id) {
        return (root, query, cb) -> {
            if (id == null) {
                return cb.conjunction();
            }
            return cb.equal(root.get("id"), id);
        };
    }
}
