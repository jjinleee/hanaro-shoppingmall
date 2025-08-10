package com.ijin.hanaro.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByIsDeletedFalse(Pageable pageable);
    Page<Product> findByIsDeletedFalseAndNameContainingIgnoreCase(String name, Pageable pageable);
}