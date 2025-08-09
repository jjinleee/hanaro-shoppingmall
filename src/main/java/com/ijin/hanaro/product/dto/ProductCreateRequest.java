// product/dto/ProductCreateRequest.java
package com.ijin.hanaro.product.dto;

import jakarta.validation.constraints.*;

public record ProductCreateRequest(
        @NotBlank String name,
        @PositiveOrZero Long price,
        @PositiveOrZero Integer stockQuantity,
        @Size(max = 5, message = "이미지는 최대 5장까지만 업로드 가능합니다")
        Integer imageCount // 업로드 전에 프론트/서비스에서 검증용(선택)
) {}