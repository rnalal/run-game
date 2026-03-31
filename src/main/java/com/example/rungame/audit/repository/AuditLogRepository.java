package com.example.rungame.audit.repository;

import com.example.rungame.audit.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

/*
* 관리자 감사 로그 Repository
*
* - AuditLog 엔티티에 대한 기본 CRUD 제공
* - 관리자 감사 로그 조회/관리의 진입점 역할
*
* 복잡한 조회가 필요할 경우
* - 기간별 조회
* - 관리자별 조회
* 등의 메서드를 확장 가능
* */
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
}
