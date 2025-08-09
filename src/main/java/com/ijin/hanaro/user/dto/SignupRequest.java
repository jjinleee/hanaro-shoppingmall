package com.ijin.hanaro.user.dto;

import jakarta.validation.constraints.*;

public record SignupRequest(
        @NotBlank @Size(min = 4, max = 50) @Pattern(regexp = "^[a-zA-Z0-9._-]{4,50}$", message = "아이디는 영문/숫자/._- 4~50자") String username,
        @NotBlank @Size(min = 8, max = 50) String password,
        @NotBlank @Size(min = 1, max = 100) String nickname,
        @Pattern(regexp = "^[0-9\\-]{9,15}$", message = "전화번호 형식이 올바르지 않습니다")
        String phone
) {}