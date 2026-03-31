package com.example.rungame.admin.controller;

import com.example.rungame.audit.domain.AuditLog;
import com.example.rungame.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/*
* 관리자 감사(Audit) 로그 조회 컨트롤러
*
* - 관리자 행동 이력 조회
* - 사용자 제재, 권한 변경 등 주요 관리자 작업 추적
*
* ADMIN 또는 SUPER_ADMIN 권한을 가진 관리자만 접근 가능
* */
@RestController
@RequestMapping("/rg-admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AuditController {

    //감사 로그 조회용 Repository
    private final AuditLogRepository auditLogRepository;

    /*
    * 감사 로그 목록 조회
    *
    * - 최근 로그인 작업 내역 확인
    * - 페이징 기반 조회
    * */
    @GetMapping
    public Page<AuditLog> list(@PageableDefault(size = 30, sort = "createdAt")Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
