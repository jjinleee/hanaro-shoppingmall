package com.ijin.hanaro.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesResponse(LocalDate date, int totalOrders, int totalItems, BigDecimal totalAmount) {}