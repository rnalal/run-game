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

@RestController
@RequestMapping("/rg-admin/audit")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    //감사 로그 목록 조회
    @GetMapping
    public Page<AuditLog> list(@PageableDefault(size = 30, sort = "createdAt")Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
