// product/dto/ProductResponse.java
package com.ijin.hanaro.product.dto;

import java.math.BigDecimal;
import java.util.List;

public record ProductResponse(
        Long id, String name, String description,
        BigDecimal price, Integer stockQuantity,
        String mainImagePath,
        List<String> imagePaths  // 상세 조회 시 사용
) {}