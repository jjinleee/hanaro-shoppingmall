package com.ijin.hanaro.order;

import com.ijin.hanaro.cart.CartItem;
import com.ijin.hanaro.cart.CartItemRepository;
import com.ijin.hanaro.order.dto.*;
import com.ijin.hanaro.product.Product;
import com.ijin.hanaro.product.ProductRepository;
import com.ijin.hanaro.user.User;
import com.ijin.hanaro.user.UserRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepo;
    private final OrderItemRepository orderItemRepo;
    private final CartItemRepository cartItemRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    /** 장바구니 전체를 주문으로 생성하고, 성공 시 장바구니 비움 + 재고 차감 (트랜잭션) */
    @Transactional
    public OrderCreateResponse createFromCart(String username) {
        User user = userRepo.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        List<CartItem> cartItems = cartItemRepo.findByCartUserId(user.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalStateException("장바구니가 비어 있어 주문을 생성할 수 없습니다.");
        }

        // 재고/삭제 체크 + 가격 합 계산
        BigDecimal total = BigDecimal.ZERO;
        Map<Long, Product> productMap = new HashMap<>();
        for (CartItem ci : cartItems) {
            Product p = productRepo.findById(ci.getProduct().getId())
                    .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + ci.getProduct().getId()));
            if (p.isDeleted()) throw new IllegalStateException("삭제된 상품이 포함되어 있습니다. 상품 id=" + p.getId());
            if (p.getStockQuantity() <= 0) throw new IllegalStateException("품절 상품이 포함되어 있습니다. 상품 id=" + p.getId());
            if (ci.getQuantity() > p.getStockQuantity()) {
                throw new IllegalStateException("재고 부족: 상품(" + p.getName() + ") 남은 재고=" + p.getStockQuantity());
            }
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
            productMap.put(p.getId(), p);
        }

        // 주문 헤더 생성
        Order order = Order.builder()
                .orderNo(generateOrderNo())
                .user(user)
                .status(OrderStatus.ORDERED)
                .paidAt(LocalDateTime.now())
                .totalPrice(total)
                .statusUpdatedAt(LocalDateTime.now())
                .build();
        orderRepo.save(order);

        // 아이템 생성 + 재고 차감
        for (CartItem ci : cartItems) {
            Product p = productMap.get(ci.getProduct().getId());

            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .product(p)
                    .productName(p.getName())
                    .unitPrice(p.getPrice())
                    .quantity(ci.getQuantity())
                    .build();
            orderItemRepo.save(oi);

            p.setStockQuantity(p.getStockQuantity() - ci.getQuantity());
            productRepo.save(p);
        }

        // 장바구니 비우기
        cartItemRepo.deleteByCartUserId(user.getId());

        return new OrderCreateResponse(order.getId(), order.getOrderNo());
    }

    private String generateOrderNo() {
        // yyyyMMddHHmmss + 4자리 난수
        String ts = java.time.format.DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int r = (int)(Math.random() * 9000) + 1000;
        return ts + r;
    }

    @Transactional(readOnly = true)
    // 내 주문 목록
    public Page<OrderListItemResponse> myOrders(String username, Pageable pageable) {
        Long userId = userRepo.findIdByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        Page<Order> ordersPage = orderRepo.findByUser_IdOrderByIdDesc(userId, pageable);

        // 현재 페이지 주문 ID 수집
        List<Long> orderIds = ordersPage.getContent().stream()
                .map(Order::getId)
                .toList();

        // 아이템 일괄 조회 후 주문별 그룹핑
        Map<Long, List<OrderItemResponse>> itemsByOrderId;
        if (orderIds.isEmpty()) {
            itemsByOrderId = Collections.emptyMap();
        } else {
            var items = orderItemRepo.findByOrder_IdIn(orderIds);
            itemsByOrderId = items.stream()
                    .collect(Collectors.groupingBy(
                            it -> it.getOrder().getId(),
                            Collectors.mapping(it -> new OrderItemResponse(
                                    it.getId(),
                                    it.getProduct().getId(),
                                    it.getProductName(),
                                    it.getUnitPrice(),
                                    it.getQuantity(),
                                    it.getUnitPrice().multiply(BigDecimal.valueOf(it.getQuantity()))
                            ), Collectors.toList())
                    ));
        }

        // 페이지 매핑 (사용자 전용 DTO: userId 제외)
        return ordersPage.map(o -> new OrderListItemResponse(
                o.getId(),
                o.getOrderNo(),
                o.getStatus(),
                o.getTotalPrice(),
                o.getCreatedAt(),
                itemsByOrderId.getOrDefault(o.getId(), List.of())
        ));
    }

    @Transactional(readOnly = true)
    // 내 주문 상세 (본인 소유 체크는 컨트롤러에서)
    public OrderDetailResponse getDetail(Long orderId) {
        Order o = orderRepo.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("주문을 찾을 수 없습니다. id=" + orderId));
        return new OrderDetailResponse(
                o.getId(), o.getOrderNo(), o.getStatus(), o.getTotalPrice(), o.getCreatedAt(),
                o.getItems().stream().map(i ->
                        new OrderItemResponse(
                                i.getId(),
                                i.getProduct().getId(),
                                i.getProductName(),
                                i.getUnitPrice(),
                                i.getQuantity(),
                                i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))
                        )
                ).collect(Collectors.toList())
        );
    }

    @Transactional(readOnly = true)
    public Page<Order> adminSearch(OrderAdminSearch cond, Pageable pageable) {
        Specification<Order> spec = Specification.where(OrderSpecifications.statusEq(cond.status()))
                .and(OrderSpecifications.orderNoLike(cond.orderNoLike()))
                .and(OrderSpecifications.usernameLike(cond.usernameLike()))
                .and(OrderSpecifications.createdBetween(cond.fromDate(), cond.toDate()));
        return orderRepo.findAll(spec, pageable);
    }
}