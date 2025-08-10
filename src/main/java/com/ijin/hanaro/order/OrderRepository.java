package com.ijin.hanaro.order;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Page<Order> findByUser_IdOrderByIdDesc(Long userId, Pageable pageable);
    Optional<Order> findByIdAndUser_Id(Long id, Long userId); //본인 주문 단건 조회용

}