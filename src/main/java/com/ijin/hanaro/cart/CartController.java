package com.ijin.hanaro.cart;

import com.ijin.hanaro.user.UserRepository;
import org.springframework.security.core.Authentication;
import com.ijin.hanaro.cart.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequiredArgsConstructor
@Tag(name = "Cart", description = "장바구니 담기/수정/삭제/조회 API")
public class CartController {

    private final CartService cartService;
    private final UserRepository userRepository;

    private Long currentUserId(Authentication authentication) {
        String username = authentication.getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + username))
                .getId();
    }

    /** 담기/증가 */
    @PostMapping("/cart/items")
    @Operation(summary = "장바구니 담기", description = "상품을 장바구니에 추가. 재고 0 또는 부족 시 BUSINESS_ERROR 응답")
    public ResponseEntity<Void> add(@RequestBody @Valid CartAddRequest req,
                                    Authentication authentication) {
        Long userId = currentUserId(authentication);
        cartService.add(userId, req.productId(), req.quantity());
        return ResponseEntity.noContent().build();
    }

    /** 조회 */
    @GetMapping("/cart")
    @Operation(summary = "장바구니 조회", description = "현재 사용자 장바구니 아이템과 합계를 조회")
    public CartResponse get(Authentication authentication) {
        return cartService.get(currentUserId(authentication));
    }

    /** 수량 변경 (0 이하이면 삭제) */
    @PatchMapping("/cart/items/{itemId}")
    @Operation(summary = "장바구니 수량 변경", description = "아이템 수량을 변경. 수량이 0 이하이면 삭제 처리")
    public ResponseEntity<Void> update(@PathVariable Long itemId,
                                       @RequestBody @Valid CartUpdateRequest req,
                                       Authentication authentication) {
        cartService.update(currentUserId(authentication), itemId, req.quantity());
        return ResponseEntity.noContent().build();
    }

    /** 제거 */
    @DeleteMapping("/cart/items/{itemId}")
    @Operation(summary = "장바구니 아이템 삭제", description = "아이템 단건 삭제")
    public ResponseEntity<Void> remove(@PathVariable Long itemId,
                                       Authentication authentication) {
        cartService.remove(currentUserId(authentication), itemId);
        return ResponseEntity.noContent().build();
    }
}