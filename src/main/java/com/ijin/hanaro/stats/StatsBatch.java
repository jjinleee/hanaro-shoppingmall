package com.ijin.hanaro.stats;

import com.ijin.hanaro.order.Order;
import com.ijin.hanaro.order.OrderItem;
import com.ijin.hanaro.order.OrderRepository;
import com.ijin.hanaro.order.OrderStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
    private static final Logger productLog = LoggerFactory.getLogger("business_product");

    private final OrderRepository orderRepo;
    private final DailySalesRepository dailyRepo;
    private final DailyProductSalesRepository productDailyRepo;

    @PersistenceContext
    private EntityManager em;

    /** 매일 00:10에 어제자 매출 집계 저장 */
    @Scheduled(cron = "0 10 0 * * *")
    public void aggregateYesterday() {
        LocalDate target = LocalDate.now().minusDays(1);
        aggregateFor(target);
    }

    /** 특정 일자 매출 집계 저장 (수동 트리거/테스트용) */
    public void aggregateFor(LocalDate target) {
        LocalDateTime start = target.atStartOfDay();
        LocalDateTime end = target.plusDays(1).atStartOfDay().minusNanos(1);

        em.flush(); // ensure newly persisted orders are visible before querying
        List<Order> orders = orderRepo.findByStatusAndPaidAtBetween(OrderStatus.DELIVERED, start, end);

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
            productLog.info("aggregated date={} productId={} qty={} amount={}", target, pid, qtyByProduct.get(pid), amtByProduct.get(pid));
        }

        log.info("aggregated date={} orders={} items={} amount={}", target, totalOrders, totalItems, totalAmount);
    }


}