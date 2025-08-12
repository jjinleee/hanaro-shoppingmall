package com.ijin.hanaro.cart;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    // PK == userId
    @Modifying
    @Transactional
    @Query("delete from Cart c where c.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}