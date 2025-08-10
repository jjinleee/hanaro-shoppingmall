package com.ijin.hanaro.cart;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartUserId(Long cartUserId);
    Optional<CartItem> findByCartUserIdAndProduct_Id(Long cartUserId, Long productId);
    Optional<CartItem> findByIdAndCartUserId(Long id, Long cartUserId);
    void deleteByCartUserId(Long userId);


}