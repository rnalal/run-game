package com.example.rungame.admin.dto;

public record AdminBanRequest(
        boolean ban,
        String reason
) { }
