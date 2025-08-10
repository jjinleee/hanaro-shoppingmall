// product/dto/ProductCreateRequest.java
package com.ijin.hanaro.product.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record ProductCreateRequest(
        @NotBlank @Size(max=200) String name,
        @NotNull @PositiveOrZero @Digits(integer=10, fraction=2) BigDecimal price,
        @NotNull @PositiveOrZero @Max(999_999) Integer stockQuantity,
        String description
) {}