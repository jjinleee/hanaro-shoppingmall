package com.ijin.hanaro.stats;

import com.ijin.hanaro.order.*;
import com.ijin.hanaro.product.Product;
import com.ijin.hanaro.product.ProductRepository;
import com.ijin.hanaro.user.User;
import com.ijin.hanaro.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Profile("local")
@RestController
@RequestMapping("/admin/debug")
@PreAuthorize("permitAll()")
public class AdminSampleDataController {

    private static final Logger bizOrderLog = LoggerFactory.getLogger("business_order");
    private final UserRepository userRepo;
    private final ProductRepository productRepo;
    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final StatsBatch statsBatch;
    private final Random random = new Random();

    public AdminSampleDataController(UserRepository userRepo,
                                     ProductRepository productRepo,
                                     OrderRepository orderRepo,
                                     OrderItemRepository orderItemRepo,
                                     StatsBatch statsBatch) {
        this.userRepo = userRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.orderItemRepo = orderItemRepo;
        this.statsBatch = statsBatch;
    }

    @PostMapping("/seed")
    @Operation(
            summary = "(local) 매출 통계용 샘플 데이터 생성 + 즉시 집계",
            description = """
        startDate부터 days일 동안 매일 ordersPerDay개의 주문을 생성합니다.
        각 주문은 DELIVERED 상태이며 해당 날짜로 createdAt/paidAt/statusUpdatedAt을 설정합니다.
        생성이 끝나면 날짜별로 StatsBatch.aggregateFor()를 호출해 통계를 즉시 적재합니다.
        
        파라미터:
        - startDate (yyyy-MM-dd) : 시작 날짜 (필수)
        - days (기본 3)          : 생성 일수
        - ordersPerDay (기본 5)  : 하루 주문 개수
        """
    )
    @Transactional
    public String seed(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(defaultValue = "3") int days,
            @RequestParam(defaultValue = "5") int ordersPerDay
    ) {
        // 1) 샘플 사용자/상품 준비
        User user = userRepo.findByUsername("sample_user").orElseGet(() -> {
            User u = new User();
            u.setUsername("sample_user");
            u.setPassword("{noop}pass"); // 인증 안 쓸 예정
            u.setNickname("샘플유저");
            u.setEnabled(true);
            u.setRole(User.Role.ROLE_USER);
            return userRepo.save(u);
        });

        List<Product> products = productRepo.findAll();
        if (products.isEmpty()) {
            // 최소 3개 생성
            for (int i = 1; i <= 3; i++) {
                Product p = new Product();
                p.setName("샘플상품" + i);
                p.setDescription("집계 테스트용 상품 " + i);
                p.setPrice(new BigDecimal(5000 * i));
                p.setStockQuantity(1000);
                products.add(productRepo.save(p));
            }
        }

        // 2) 날짜별 주문 생성
        for (int d = 0; d < days; d++) {
            LocalDate target = startDate.plusDays(d);
            for (int i = 0; i < ordersPerDay; i++) {
                createDeliveredOrderForDay(user, products, target);
            }
            // 3) 해당 일자 통계 즉시 집계
            statsBatch.aggregateFor(target);
        }
        return "OK";
    }

    @PostMapping("/aggregate")
    @Operation(
            summary = "(local) 특정 일자 매출 통계 수동 집계",
            description = "date 파라미터(yyyy-MM-dd)로 전달된 일자를 StatsBatch.aggregateFor(date)로 즉시 집계합니다."
    )
    @Transactional
    public String aggregateManual(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        statsBatch.aggregateFor(date);
        return "OK";
    }

    private void createDeliveredOrderForDay(User user, List<Product> products, LocalDate date) {
        LocalDateTime when = date.atTime(10 + random.nextInt(10), random.nextInt(60));
        Order order = new Order();
        order.setUser(user);
        order.setOrderNo("SEED-" + System.currentTimeMillis() + "-" + random.nextInt(10000));
        order.setStatus(OrderStatus.DELIVERED);
        order.setPaidAt(when);
        order.setStatusUpdatedAt(when);
        order.setCreatedAt(when);
        order.setTotalPrice(BigDecimal.ZERO);
        order = orderRepo.save(order);

        int itemCount = 1 + random.nextInt(3); // 1~3개
        BigDecimal total = BigDecimal.ZERO;
        for (int k = 0; k < itemCount; k++) {
            Product p = products.get(random.nextInt(products.size()));
            int qty = 1 + random.nextInt(3);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(p);
            item.setProductName(p.getName());
            item.setUnitPrice(p.getPrice());
            item.setQuantity(qty);
            orderItemRepo.save(item);

            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(qty)));
        }
        order.setTotalPrice(total);
        orderRepo.save(order);

        bizOrderLog.info("[Seed] order id={} date={} items={} amount={}",
                order.getId(), date, itemCount, total);
    }
}