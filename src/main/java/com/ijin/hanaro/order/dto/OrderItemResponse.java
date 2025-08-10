package com.ijin.hanaro.order.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long itemId,
        Long productId,
        String productName,
        BigDecimal unitPrice,
        Integer quantity,
        BigDecimal lineTotal
) {}