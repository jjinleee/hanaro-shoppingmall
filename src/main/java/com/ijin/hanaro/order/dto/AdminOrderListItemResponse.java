package com.ijin.hanaro.order.dto;

import com.ijin.hanaro.order.OrderStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/** 관리자 전용 주문 목록 DTO (userId 포함) */
public record AdminOrderListItemResponse(
        Long id,
        String orderNo,
        OrderStatus status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        Long userId,
        List<OrderItemResponse> items
) {}