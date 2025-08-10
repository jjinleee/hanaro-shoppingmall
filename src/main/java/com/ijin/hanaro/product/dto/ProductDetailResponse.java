package com.ijin.hanaro.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductDetailResponse(
        Long id,
        String name,
        BigDecimal price,
        String description,
        int stockQuantity,
        String mainImagePath,
        List<String> imagePaths
) {}