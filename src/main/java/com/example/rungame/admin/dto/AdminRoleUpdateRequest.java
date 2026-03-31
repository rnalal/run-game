package com.example.rungame.admin.dto;

/*
* 관리자 사용자 권한 변경 요쳥 DTO
*
* - 사용자의 시스템 권한(Role)을 변경하기 위한 요청 데이터
* - SUPER_ADMIN 권한에서만 사용 가능
* */
public record AdminRoleUpdateRequest(
        /*
        * 변경할 사용자 권한
        *
        * 허용 값:
        * - "USER"
        * - "ADMIN"
        * - "SUPER_ADMIN"
        * */
        String role
) { }
