package com.ijin.hanaro.product.dto;

public record StockAdjustResponse(
        Long productId,
        int newStock
) {}