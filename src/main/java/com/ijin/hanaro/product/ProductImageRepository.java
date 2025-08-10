// product/ProductImageRepository.java
package com.ijin.hanaro.product;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ProductImageRepository extends JpaRepository<ProductImage, Long> {
    @Query("select coalesce(sum(pi.sizeBytes),0) from ProductImage pi where pi.product.id = :productId")
    long sumSizeByProductId(Long productId);

    long countByProduct_Id(Long productId);

    List<ProductImage> findByProduct_Id(Long productId);
}