package com.example.rungame.admin.controller;

import com.example.rungame.admin.dto.*;
import com.example.rungame.admin.service.AdminUserService;
import jakarta.annotation.security.PermitAll;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/rg-admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    //관리자 사용자 관리 페이지(view)
    @GetMapping("/page")
    @PermitAll
    public String page() {
        return "admin/admin-users";
    }

    //사용자 목록 조회
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

    //사용자 상세 정보 조회
    @GetMapping("/{userId}")
    @ResponseBody
    public AdminUserDetailResponse detail(@PathVariable Long userId){
        return adminUserService.getUserDetail(userId);
    }

    //사용자 정지/ 정지 해제
    @PatchMapping("/{userId}/ban")
    @ResponseBody
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    public java.util.Map<String, Object> ban(@PathVariable Long userId, @RequestBody AdminBanRequest req) {
        adminUserService.updateBan(userId, req.ban());
        return java.util.Map.of("success", true);
    }

    //사용자 권한 변경
    @PatchMapping("/{userId}/role")
    @ResponseBody
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public java.util.Map<String, Object> updateRole(@PathVariable Long userId, @RequestBody AdminRoleUpdateRequest req){
        adminUserService.updateRole(userId, req.role());
        return java.util.Map.of("success", true);
    }

    //사용자 활동 이력 조회
    @GetMapping("/{userId}/activity")
    @ResponseBody
    public AdminUserActivityResponse activity(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "30") int days
    ) {
        return adminUserService.getUserActivity(userId, days);
    }
}
