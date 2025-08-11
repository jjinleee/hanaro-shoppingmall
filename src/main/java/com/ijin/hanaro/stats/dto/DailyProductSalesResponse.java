package com.ijin.hanaro.stats.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyProductSalesResponse(LocalDate date, Long productId, int qty, BigDecimal amount) {}