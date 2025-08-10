package com.ijin.hanaro.cart;

import com.ijin.hanaro.cart.dto.*;
import com.ijin.hanaro.product.Product;
import com.ijin.hanaro.product.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepo;
    private final CartItemRepository itemRepo;
    private final ProductRepository productRepo;

    /** 담기/증가 (같은 상품이면 수량 합치기) */
    @Transactional
    public void add(Long userId, Long productId, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("수량은 1 이상이어야 합니다.");

        // 카트 없으면 생성
        cartRepo.findById(userId).orElseGet(() -> cartRepo.save(new Cart(userId)));

        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다."));
        if (product.isDeleted()) throw new IllegalArgumentException("삭제된 상품입니다.");
        if (product.getStockQuantity() <= 0) {
            throw new IllegalArgumentException("품절된 상품은 장바구니에 담을 수 없습니다.");
        }

        CartItem item = itemRepo.findByCartUserIdAndProduct_Id(userId, productId)
                .orElse(null);

        int newQty = quantity;
        if (item != null) newQty = item.getQuantity() + quantity;

        if (newQty > product.getStockQuantity()) {
            throw new IllegalArgumentException("재고를 초과하여 담을 수 없습니다. (재고: " + product.getStockQuantity() + ")");
        }

        if (item == null) {
            itemRepo.save(new CartItem(userId, product, newQty));
        } else {
            item.setQuantity(newQty);
            itemRepo.save(item);
        }
    }

    /** 내 카트 조회 */
    @Transactional(readOnly = true)
    public CartResponse get(Long userId) {
        List<CartItem> items = itemRepo.findByCartUserId(userId);

        List<CartItemResponse> rows = items.stream().map(ci -> {
            BigDecimal price = ci.getProduct().getPrice();
            BigDecimal line = price.multiply(BigDecimal.valueOf(ci.getQuantity()));
            return new CartItemResponse(
                    ci.getId(),
                    ci.getProduct().getId(),
                    ci.getProduct().getName(),
                    price,
                    ci.getQuantity(),
                    line
            );
        }).toList();

        BigDecimal total = rows.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(rows, total);
    }

    /** 수량 변경 (0 이하이면 삭제) */
    @Transactional
    public void update(Long userId, Long itemId, int quantity) {
        CartItem item = itemRepo.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("카트 아이템이 존재하지 않습니다."));

        if (quantity <= 0) {
            itemRepo.delete(item);
            return;
        }

        Product product = item.getProduct();
        if (product.isDeleted()) {
            throw new IllegalArgumentException("삭제된 상품입니다.");
        }
        if (quantity > product.getStockQuantity()) {
            throw new IllegalArgumentException("재고를 초과하여 담을 수 없습니다. (재고: " + product.getStockQuantity() + ")");
        }

        item.setQuantity(quantity);
        itemRepo.save(item);
    }

    /** 항목 제거 */
    @Transactional
    public void remove(Long userId, Long itemId) {
        CartItem item = itemRepo.findByIdAndCartUserId(itemId, userId)
                .orElseThrow(() -> new IllegalArgumentException("카트 아이템이 존재하지 않습니다."));
        itemRepo.delete(item);
    }
}