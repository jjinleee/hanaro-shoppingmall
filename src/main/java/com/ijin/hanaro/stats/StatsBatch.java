package com.ijin.hanaro.stats;

import com.ijin.hanaro.order.Order;
import com.ijin.hanaro.order.OrderItem;
import com.ijin.hanaro.order.OrderRepository;
import com.ijin.hanaro.order.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StatsBatch {
    private static final Logger log = LoggerFactory.getLogger("business_order");

    private final OrderRepository orderRepo;
    private final DailySalesRepository dailyRepo;
    private final DailyProductSalesRepository productDailyRepo;

    /** 매일 00:10에 어제자 매출 집계 저장 */
    @Scheduled(cron = "0 10 0 * * *")
    public void aggregateYesterday() {
        LocalDate target = LocalDate.now().minusDays(1);
        LocalDateTime start = target.atStartOfDay();
        LocalDateTime end = target.plusDays(1).atStartOfDay().minusNanos(1);

        // 배송완료(DELIVERED) 기준으로 집계 (요구가 다르면 ORDERED 등으로 변경)
        List<Order> orders = orderRepo.findByStatusAndCreatedAtBetween(OrderStatus.DELIVERED, start, end);

        int totalOrders = orders.size();
        int totalItems = 0;
        BigDecimal totalAmount = BigDecimal.ZERO;

        Map<Long, Integer> qtyByProduct = new HashMap<>();
        Map<Long, BigDecimal> amtByProduct = new HashMap<>();

        for (Order o : orders) {
            var items = o.getItems();
            for (OrderItem it : items) {
                totalItems += it.getQuantity();
                BigDecimal line = it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()));
                totalAmount = totalAmount.add(line);

                qtyByProduct.merge(it.getProduct().getId(), it.getQuantity(), Integer::sum);
                amtByProduct.merge(it.getProduct().getId(), line, BigDecimal::add);
            }
        }

        dailyRepo.save(new DailySales(target, totalOrders, totalItems, totalAmount));
        for (Long pid : qtyByProduct.keySet()) {
            productDailyRepo.save(new DailyProductSales(
                    target, pid, qtyByProduct.get(pid), amtByProduct.get(pid)
            ));
        }

        log.info("aggregated date={} orders={} items={} amount={}", target, totalOrders, totalItems, totalAmount);
    }
}