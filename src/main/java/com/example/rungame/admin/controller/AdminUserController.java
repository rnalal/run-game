package com.example.rungame.admin.controller;

import com.example.rungame.admin.dto.*;
import com.example.rungame.admin.service.AdminUserService;
import com.example.rungame.audit.aop.AdminAction;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

/*
* 관리자 사용자 관리 컨트롤러
*
* - 사용자 목록 조회 (검색+페이징)
* - 사용자 상세 정보 조회
* - 사용자 정지/정지 해제
* - 사용자 권한 변경
* - 사용자 활동 이력 조회
*
* ADMIN/SUPER_ADMIN 권한을 가진 관리자만 접근 가능
* */
@Controller
@RequestMapping("/rg-admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminUserController {

    //관리자 사용자 관리 관련 비즈니스 로직
    private final AdminUserService adminUserService;

    /*
    * 관리자 사용자 관리 페이지(view)
    *
    * - 화면 접근은 허용하되, 실제 데이터 API는 권한 체크를 통해 보호
    * */
    @GetMapping("/page")
    @PermitAll
    public String page() {
        return "admin/admin-users";
    }

    /*
    * 사용자 목록 조회 (검색+페이징)
    *
    * - ID, 닉네임, 이메일, 상태, 권한 조건으로 검색 가능
    * - 관리자 사용자 관리 테이블 데이터 제공
    * */
    @GetMapping("/data")
    @ResponseBody
    public Page<AdminUserResponse> list(
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String status,  // ACTIVE, BANNED, DELETED
            @RequestParam(required = false) String role,    // USER, ADMIN, SUPER_ADMIN
            @PageableDefault(size = 20, sort = "id") Pageable pageable
            ) {
        return adminUserService.searchUsers(id, nickname, email, status, role, pageable);
    }

    /*
    * 사용자 상세 정보 조회
    *
    * - 기본 정보
    * - 상태 및 권한
    * */
    @GetMapping("/{userId}")
    @ResponseBody
    public AdminUserDetailResponse detail(@PathVariable Long userId){
        return adminUserService.getUserDetail(userId);
    }

    /*
    * 사용자 정지/ 정지 해제
    *
    * - 관리자에 의한 계정 제재 토글
    * - 감사 로그(AdminAction AOP) 기록 대상
    * */
    @PatchMapping("/{userId}/ban")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @AdminAction(value = "USER_BAN_TOGGLE", resource = "user")
    public java.util.Map<String, Object> ban(@PathVariable Long userId, @RequestBody AdminBanRequest req) {
        adminUserService.updateBan(userId, req.ban());
        return java.util.Map.of("success", true);
    }

    /*
    * 사용자 권한 변경
    *
    * - SUPER_ADMIN 전용 기능
    * - 권한 변경 이력은 감사 로그로 기록
    * */
    @PatchMapping("/{userId}/role")
    @ResponseBody
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    @AdminAction(value = "USER_ROLE_UPDATE", resource = "user")
    public java.util.Map<String, Object> updateRole(@PathVariable Long userId, @RequestBody AdminRoleUpdateRequest req){
        adminUserService.updateRole(userId, req.role());
        return java.util.Map.of("success", true);
    }

    /*
    * 사용자 활동 이력 조회
    *
    * - 최근 N일 기준 세션/ 이벤트 활동 요약
    * */
    @GetMapping("/{userId}/activity")
    @ResponseBody
    public AdminUserActivityResponse activity(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") int days
    ) {
        return adminUserService.getUserActivity(userId, days);
    }
}
