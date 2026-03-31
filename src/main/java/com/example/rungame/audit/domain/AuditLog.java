package com.example.rungame.audit.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/*
 * 관리자 감사 로그 엔티티
 *
 * - 관리자가 수행한 주요 행위를 영구적으로 기록
 * - 보안 감사, 운영 추적, 장애/이슈 분석 용도로 사용
 *
 * AOP(@AdminAction)와 결합되어
 * 비즈니스 로직 수정 없이 자동으로 로그가 남도록 설계됨
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "admin_logs", indexes = {
        /*
        * 생성 시각 기준 조회 성능 향상
        * - 최신 로그 조회, 기간별 조회 용도
        * */
        @Index(name="idx_audit_created_at", columnList = "createdAt"),
        /*
        * actor 기준 조회 성능 향상
        * - 특정 관리자 행동 추적 용도
        * */
        @Index(name="idx_audit_actor", columnList = "actor")
})
public class AuditLog {
    //감사 로그 ID
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
    * actor 식별자
    * - 관리자 이메일 또는 ID
    * - SecurityContext에서 추출
    * */
    private String actor;
    /*
    * 수행된 관리자 활동
    * - ex) USER_BAN, USER_ROLE_UPDATE
    * */
    private String action;
    /*
    * 액션 대상 리소스
    * - ex) user:123, leaderboard
    * */
    private String resource;
    /*
    * 상세 정보
    *
    * - 메서드 정보, 파라미터 요약 등
    * - 길이가 길어질 수 있으므로 LOB으로 저장
    * */
    @Lob
    private String details;

    //로그 생성 시각
    private LocalDateTime createdAt;

    //엔티티 저장 전 자동으로 생성 시각 설정
    @PrePersist
    void prePersist(){
        createdAt = LocalDateTime.now();
    }
}
