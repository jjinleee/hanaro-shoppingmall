package com.ijin.hanaro.cart.dto;

public record CartUpdateRequest(
        int quantity // 0 이하면 해당 아이템 삭제
) {}