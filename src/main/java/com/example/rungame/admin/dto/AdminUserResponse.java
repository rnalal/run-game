package com.example.rungame.admin.dto;

import com.example.rungame.user.domain.User;
import lombok.Value;

import java.time.LocalDateTime;

/*
* 관리자 사용자 목록 응답 DTO
*
* - 관리자 사용자 목록 테이블에 표시되는 기본 정보
* - 상세 조회가 아닌 리스트 전용 경량 DTO
* */
@Value
public class AdminUserResponse {

    //========사용자 정보============
    Long id;
    String nickname;
    String email;
    String role;
    String status;
    LocalDateTime createdAt;
    LocalDateTime lastLoginAt;

    /*
    * User 엔티티 -> AdminUserResponse 변환
    *
    * - 관리자 목록 조회 시 필요한 필드만 선별
    * */
    public static AdminUserResponse from(User u) {
        return new AdminUserResponse(
                u.getId(),
                u.getNickname(),
                u.getEmail(),
                u.getRole(),
                u.getStatus(),
                u.getCreatedAt(),
                u.getLastLoginAt()
        );
    }
}
