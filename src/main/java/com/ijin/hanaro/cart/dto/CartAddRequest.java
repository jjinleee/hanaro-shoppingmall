package com.ijin.hanaro.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartAddRequest(
        @NotNull Long productId,
        @Min(1) int quantity
) {}