package com.ijin.hanaro.product.dto;

import java.math.BigDecimal;

public record ProductListItemResponse(
        Long id,
        String name,
        BigDecimal price,
        String mainImagePath
) {}