package com.ijin.hanaro.auth.dto;

public record LoginResponse(String accessToken, String tokenType, long expiresInSec) {}
