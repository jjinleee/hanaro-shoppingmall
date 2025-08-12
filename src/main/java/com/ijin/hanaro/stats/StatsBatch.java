package com.ijin.hanaro.stats;

import com.ijin.hanaro.product.Product;
import com.ijin.hanaro.product.ProductRepository;
import com.ijin.hanaro.user.User;
import com.ijin.hanaro.user.UserRepository;
import com.ijin.hanaro.order.OrderItemRepository;
import java.util.concurrent.ThreadLocalRandom;

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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class StatsBatch {
    private static final Logger orderLog = LoggerFactory.getLogger("business.order");
    private static final Logger productLog = LoggerFactory.getLogger("business.product");

    private final OrderRepository orderRepo;
    private final DailySalesRepository dailyRepo;
    private final DailyProductSalesRepository productDailyRepo;
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OrderItemRepository orderItemRepo;

    @PersistenceContext
    private EntityManager em;

    /** 매일 00:10에 어제자 매출 집계 저장 */
    @Scheduled(cron = "0 10 0 * * *", zone = "Asia/Seoul")
    public void aggregateYesterday() {
        LocalDate target = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1);
        aggregateFor(target);
    }

    /** 특정 일자 매출 집계 저장 (수동 트리거/테스트용) */
    @Transactional
    public void aggregateFor(LocalDate target) {
        long t0 = System.currentTimeMillis();
        orderLog.info("BATCH_DAILY_SALES_BEGIN date={}", target);
        // idempotency: remove existing aggregates for the day
        dailyRepo.deleteByStatDate(target);
        productDailyRepo.deleteByStatDate(target);
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

        orderLog.info("BATCH_DAILY_SALES_DONE date={} orders={} items={} amount={} tookMs={}", target, totalOrders, totalItems, totalAmount, (System.currentTimeMillis() - t0));
    }


    /**
     * 테스트용 데이터 삽입: 지정한 날짜의 임의 시간에 DELIVERED 주문 n건을 생성한다.
     * - username: 주문 소유자 (존재해야 함)
     * - statDate: 매출 통계 대상 일자 (예: LocalDate.now())
     * - count: 생성할 주문 수
     * - maxItemsPerOrder: 주문당 최대 상품 종류 수(1..max)
     * 반환값: 실제 생성된 주문 수
     */
    @Transactional
    public int seedDeliveredOrders(String username, java.time.LocalDate statDate, int count, int maxItemsPerOrder) {
        Objects.requireNonNull(username, "username");
        Objects.requireNonNull(statDate, "statDate");
        if (count <= 0) return 0;
        if (maxItemsPerOrder <= 0) maxItemsPerOrder = 3;

        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username));

        List<Product> candidates = productRepo.findAll().stream()
                .filter(p -> !p.isDeleted() && p.getStockQuantity() > 0)
                .toList();
        if (candidates.isEmpty()) {
            throw new IllegalStateException("주문을 생성할 상품이 없습니다. (삭제되지 않고 재고>0)");
        }

        java.time.LocalDateTime start = statDate.atStartOfDay();
        java.time.LocalDateTime end = statDate.plusDays(1).atStartOfDay();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        int created = 0;
        for (int i = 0; i < count; i++) {
            // 주문 시간: [start, end) 범위 내 임의 시간
            long startEpoch = start.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endEpoch = end.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
            long ts = rnd.nextLong(startEpoch, endEpoch);
            java.time.LocalDateTime paidAt = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(ts), java.time.ZoneId.systemDefault());

            // 주문에 담을 상품 수
            int kinds = Math.max(1, rnd.nextInt(1, Math.min(maxItemsPerOrder, Math.max(2, candidates.size())) + 1));
            Collections.shuffle(candidates);
            List<Product> pick = candidates.subList(0, kinds);

            java.math.BigDecimal total = java.math.BigDecimal.ZERO;
            Order order = Order.builder()
                    .user(user)
                    .status(OrderStatus.DELIVERED)
                    .createdAt(paidAt)
                    .paidAt(paidAt)
                    .orderNo("SEED-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .totalPrice(java.math.BigDecimal.ZERO)
                    .build();
            orderRepo.save(order);

            for (Product p : pick) {
                int maxQty = Math.max(1, Math.min(3, p.getStockQuantity()));
                int qty = rnd.nextInt(1, maxQty + 1);
                java.math.BigDecimal line = p.getPrice().multiply(java.math.BigDecimal.valueOf(qty));

                OrderItem oi = OrderItem.builder()
                        .order(order)
                        .product(p)
                        .productName(p.getName())
                        .unitPrice(p.getPrice())
                        .quantity(qty)
                        .build();
                orderItemRepo.save(oi);

                // 재고 차감
                p.setStockQuantity(p.getStockQuantity() - qty);
                productRepo.save(p);

                total = total.add(line);
            }

            order.setTotalPrice(total);
            orderRepo.save(order);

            ORDER_STATUS_LOG(order, "SEEDED_DELIVERED");
            created++;
        }

        orderLog.info("SEED_ORDERS_DONE date={} username={} created={}", statDate, username, created);
        return created;
    }

    /** 편의 오버로드: 주문당 최대 3종 */
    @Transactional
    public int seedDeliveredOrders(String username, java.time.LocalDate statDate, int count) {
        return seedDeliveredOrders(username, statDate, count, 3);
    }

    private void ORDER_STATUS_LOG(Order order, String tag) {
        orderLog.info("{} id={} orderNo={} paidAt={} total={}", tag, order.getId(), order.getOrderNo(), order.getPaidAt(), order.getTotalPrice());
    }
}