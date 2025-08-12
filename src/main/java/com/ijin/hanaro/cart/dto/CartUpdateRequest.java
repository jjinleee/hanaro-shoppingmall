package com.ijin.hanaro.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartUpdateRequest(
        @NotNull(message = "quantity는 필수입니다.")
        @Min(value = 0, message = "quantity는 0 이상이어야 합니다.")   // 0이면 삭제 처리 허용
        @Max(value = 9999, message = "quantity는 9,999 이하로 입력하세요.")
        Integer quantity
) {}