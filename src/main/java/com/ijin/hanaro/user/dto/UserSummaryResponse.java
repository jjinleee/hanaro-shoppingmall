package com.ijin.hanaro.user.dto;

public record UserSummaryResponse(
        Long id,
        String username,
        String nickname,
        String phone,
        boolean enabled
) {}