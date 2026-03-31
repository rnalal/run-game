package com.example.rungame.common.security;

/*
 * 시스템 전역에서 사용하는 권한 정의
 *
 * - Spring Security의 ROLE_* 권한 체계와 매핑되는 도메인 Enum
 * - 문자열 하드코딩을 방지하고 권한을 타입으로 관리하기 위한 목적
 *
 * Security 설정, JWT Claim, 관리자 기능 전반에서 공통 사용
 */
public enum Role {
    /*
    * 일반 사용자
    * - 게임 플레이
    * - 기본 서비스 이용
    * */
    USER,
    /*
    * 관리자
    *
    * - 유저 관리
    * - 이벤트/세션 관리
    * */
    ADMIN,
    /*
    * 최고 관리자
    * - 시스템 설정
    * - 권한 변경
    * - 전체 관리 기능
    * */
    SUPER_ADMIN
}
