package com.ijin.hanaro.stats;

import com.ijin.hanaro.stats.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatsService {
    private final DailySalesRepository dailyRepo;
    private final DailyProductSalesRepository productDailyRepo;

    public List<DailySalesResponse> range(LocalDate from, LocalDate to) {
        return dailyRepo.findBySalesDateBetweenOrderBySalesDate(from, to)
                .stream()
                .map(d -> new DailySalesResponse(d.getSalesDate(), d.getTotalOrders(), d.getTotalItems(), d.getTotalAmount()))
                .toList();
    }

    public List<DailyProductSalesResponse> perProduct(LocalDate from, LocalDate to) {
        return productDailyRepo.findBySalesDateBetweenOrderBySalesDate(from, to).stream()
                .map(d -> new DailyProductSalesResponse(d.getSalesDate(), d.getProductId(), d.getQty(), d.getAmount()))
                .toList();
    }
}