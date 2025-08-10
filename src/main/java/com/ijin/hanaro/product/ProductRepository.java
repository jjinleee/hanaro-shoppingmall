package com.ijin.hanaro.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Page<Product> findByIsDeletedFalse(Pageable pageable);
    Page<Product> findByIsDeletedFalseAndNameContainingIgnoreCase(String name, Pageable pageable);

    @Query("""
        select p
        from Product p
        where p.isDeleted = false
          and (
               :q is null or :q = ''
               or lower(p.name) like lower(concat('%', :q, '%'))
               or lower(p.description) like lower(concat('%', :q, '%'))
          )
        """)
    Page<Product> searchPublic(@Param("q") String q, Pageable pageable);

    Optional<Product> findByIdAndIsDeletedFalse(Long id);
}