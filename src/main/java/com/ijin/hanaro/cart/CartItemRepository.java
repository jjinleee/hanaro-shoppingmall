package com.ijin.hanaro.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartUserId(Long cartUserId);
    Optional<CartItem> findByCartUserIdAndProduct_Id(Long cartUserId, Long productId);
    Optional<CartItem> findByIdAndCartUserId(Long id, Long cartUserId);
    @Modifying
    @Transactional
    @Query("delete from CartItem ci where ci.cartUserId = :userId")
    void deleteByCartUserId(@Param("userId") Long userId);


}