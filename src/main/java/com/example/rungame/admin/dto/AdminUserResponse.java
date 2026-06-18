package com.example.rungame.admin.dto;

import com.example.rungame.user.domain.User;
import lombok.Value;

import java.time.LocalDateTime;

@Value
public class AdminUserResponse {

    Long id;
    String nickname;
    String email;
    String role;
    String status;
    LocalDateTime createdAt;
    LocalDateTime lastLoginAt;

    //User 엔티티 -> AdminUserResponse 변환
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
