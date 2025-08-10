// product/Product.java
package com.ijin.hanaro.product;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false, length=200)
    private String name;

    @Column(columnDefinition="TEXT")
    private String description;

    @Column(nullable=false, precision=12, scale=2)
    private BigDecimal price;

    @Column(nullable=false)
    private Integer stockQuantity = 0;

    // 대표 이미지 경로(썸네일/리스트용), 없을 수 있음
    @Column(length = 500)
    private String mainImagePath;

    @Column(nullable=false)
    private boolean isDeleted = false;

    @OneToMany(mappedBy = "product", fetch = FetchType.LAZY,
            cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductImage> images = new ArrayList<>();
}