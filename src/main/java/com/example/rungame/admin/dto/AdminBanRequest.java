package com.example.rungame.admin.dto;

/*
* 관리자 사용자 정지/해제 요청 DTO
*
* - 사용자 계정 제재 여부를 전달
* - 제재 사유를 함께 전달하여
*   감사 로그 및 운영 이력 관리에 활용
* */
public record AdminBanRequest(
        /*
        * true -> 사용자 정지
        * false -> 정지 해제
        * */
        boolean ban,
        /*
        * 정지 또는 해제 사유
        * - 관리자 감사 로그(AdminAction) 기록용
        * - 운영 이력 추적을 위한 설명
        * */
        String reason
) { }
