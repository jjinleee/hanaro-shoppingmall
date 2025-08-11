package com.ijin.hanaro.stats;

import com.ijin.hanaro.stats.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/admin/stats")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService service;

    @GetMapping("/daily")
    @Operation(summary = "일별 매출 통계(집계 테이블)",
            description = "daily_sales 테이블에서 from~to 날짜 범위로 조회")
    public List<DailySalesResponse> daily(
            @Parameter(description="조회 시작일(yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description="조회 종료일(yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ){
        return service.range(from, to);
    }

    @GetMapping("/products")
    @Operation(summary = "일별 상품 매출 통계(집계 테이블)",
            description = "daily_product_sales 테이블에서 from~to 날짜 범위로 조회")
    public List<DailyProductSalesResponse> perProduct(
            @Parameter(description="조회 시작일(yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description="조회 종료일(yyyy-MM-dd)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ){
        return service.perProduct(from, to);
    }
}