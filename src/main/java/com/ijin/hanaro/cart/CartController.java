package com.ijin.hanaro.cart;

import com.ijin.hanaro.user.UserRepository;
import org.springframework.security.core.Authentication;
import com.ijin.hanaro.cart.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
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
    public ResponseEntity<Void> add(@RequestBody @Valid CartAddRequest req,
                                    Authentication authentication) {
        Long userId = currentUserId(authentication);
        cartService.add(userId, req.productId(), req.quantity());
        return ResponseEntity.noContent().build();
    }

    /** 조회 */
    @GetMapping("/cart")
    public CartResponse get(Authentication authentication) {
        return cartService.get(currentUserId(authentication));
    }

    /** 수량 변경 (0 이하이면 삭제) */
    @PatchMapping("/cart/items/{itemId}")
    public ResponseEntity<Void> update(@PathVariable Long itemId,
                                       @RequestBody @Valid CartUpdateRequest req,
                                       Authentication authentication) {
        cartService.update(currentUserId(authentication), itemId, req.quantity());
        return ResponseEntity.noContent().build();
    }

    /** 제거 */
    @DeleteMapping("/cart/items/{itemId}")
    public ResponseEntity<Void> remove(@PathVariable Long itemId,
                                       Authentication authentication) {
        cartService.remove(currentUserId(authentication), itemId);
        return ResponseEntity.noContent().build();
    }
}