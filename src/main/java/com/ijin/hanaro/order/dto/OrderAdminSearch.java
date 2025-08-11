package com.ijin.hanaro.order.dto;

import com.ijin.hanaro.order.OrderStatus;
import java.time.LocalDate;

public record OrderAdminSearch(
        OrderStatus status,     // ORDERED / PREPARING / SHIPPING / DELIVERED / CANCELED
        String orderNoLike,     // 부분일치
        String usernameLike,    // 부분일치
        LocalDate fromDate,     // createdAt 기준 시작(포함)
        LocalDate toDate        // createdAt 기준 끝(포함)
) { }