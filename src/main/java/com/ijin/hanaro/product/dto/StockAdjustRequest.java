package com.ijin.hanaro.product.dto;

import jakarta.validation.constraints.NotNull;

public record StockAdjustRequest(
        @NotNull Integer deltaQty   // +면 입고, -면 출고
) {}