// product/dto/ProductUpdateRequest.java
package com.ijin.hanaro.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductUpdateRequest(
        @Size(max = 200)
        String name,

        @PositiveOrZero
        @Digits(integer = 12, fraction = 2)
        BigDecimal price,

        @PositiveOrZero
        Integer stockQuantity,

        @Size(max = 65535)
        String description,

        @Size(max = 500)
        String mainImagePath
) {}