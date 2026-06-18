package com.example.rungame.user.domain;

import com.example.rungame.common.support.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Comment;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "email_UNIQUE", columnNames = {"email"}),
                @UniqueConstraint(name = "nickname_UNIQUE", columnNames = {"nickname"})
        }
)
@Data
@NoArgsConstructor(access = AccessLevel.PROTECTED) //JPA 기본 생성자
@AllArgsConstructor(access = AccessLevel.PRIVATE) //Builder,팩토리에서 사용
public class User extends BaseTimeEntity {

        @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Long id;

        @Comment("로그인 아이디")
        @Column(length = 190, nullable = false)
        private String email;

        @Comment("비밀번호 해시")
        @Column(name = "password_hash", length = 255, nullable = false)
        private String passwordHash;

        @Comment("닉네임")
        @Column(length = 50, nullable = false)
        private String nickname;

        @Comment("역할: USER/ADMIN/SUPER_ADMIN")
        @Column(name="role", length = 20, nullable = false)
        private String role = "USER";

        @Comment("계정 상태: ACTIVE/BANNED/DELETED")
        @Column(name = "status", length = 20, nullable = false)
        private String status = "ACTIVE"; //기본값 보장

        //로그아웃, 강제 로그아웃 시 토큰 무효화를 위한 버전 필드
        @Column(name="token_version", nullable = false)
        private int tokenVersion = 0;

        @Comment("최근 로그인 시각")
        @Column(name = "last_login_at")
        private LocalDateTime lastLoginAt;

        //로그아웃 또는 강제 로그아웃 시 기존 발급된 토큰을 모두 무효화하기 위해 버전 증가
        public void incrementTokenVersion() {
                this.tokenVersion += 1;
        }

        //새 사용자 생성용 정적 팩토리
        public static User create(String email, String passwordHash, String nickname){
                User u = new User();
                u.email = email;
                u.passwordHash = passwordHash;
                u.nickname = nickname;
                u.role = "USER";
                u.status = "ACTIVE";
                return u;
        }

        //마이페이지,관리 페이지 응답에서 자주 쓰일 계정 상태 헬퍼들
        public boolean isActive(){
            return "ACTIVE".equalsIgnoreCase(this.status);
        }
        public boolean isBanned(){
                return "BANNED".equalsIgnoreCase(this.status);
        }
        public boolean isAdminOrAbove() {
                return "ADMIN".equalsIgnoreCase(this.role) || "SUPER_ADMIN".equalsIgnoreCase(this.role);
        }
}
