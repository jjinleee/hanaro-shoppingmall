package com.ijin.hanaro.product;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name="product_images",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"storedName"}),
                @UniqueConstraint(columnNames = {"checksumSha256"})
        })
@Getter @Setter @NoArgsConstructor
public class ProductImage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="product_id", nullable=false)
    private Product product;

    @Column(nullable=false, length=255) private String originalName;
    @Column(nullable=false, length=500) private String storedPath;   // /resources/static/upload/...
    @Column(nullable=false, length=200) private String storedName;   // UUID.ext
    @Column(nullable=false)             private Integer sizeBytes;   // ≤ 524_288
    @Column(length=64)                  private String checksumSha256;
    @Column(nullable=false)             private boolean primaryImage = false;
}
