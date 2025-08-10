package com.ijin.hanaro.order.dto;

import com.ijin.hanaro.order.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderListItemResponse(
        Long id,
        String orderNo,
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt
) { }