package com.ijin.hanaro.cart.dto;

import java.math.BigDecimal;

public record CartItemResponse(
        Long itemId,
        Long productId,
        String name,
        BigDecimal unitPrice,
        int quantity,
        BigDecimal lineTotal
) {}