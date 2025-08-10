// product/ProductController.java
package com.ijin.hanaro.product;

import com.ijin.hanaro.product.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService productService;

    // ===== 공개 조회 =====
    @GetMapping("/products")
    public Page<ProductResponse> list(@RequestParam(required=false) String q,
                                      @RequestParam(defaultValue="0") int page,
                                      @RequestParam(defaultValue="10") int size) {
        return productService.list(q, PageRequest.of(page, size, Sort.by("id").descending()));
    }

    @GetMapping("/products/{id}")
    public ProductResponse detail(@PathVariable Long id) { return productService.detail(id); }

    // ===== 관리자 전용 =====
    @PostMapping(value = "/admin/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Long create(
            @RequestParam("name") String name,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stockQuantity") Integer stockQuantity,
            @RequestParam("description") String description,
            @RequestPart("mainImage") MultipartFile mainImage
    ) {
        if (mainImage == null || mainImage.isEmpty()) {
            throw new IllegalArgumentException("메인 이미지는 필수입니다.");
        }
        ProductCreateRequest request = new ProductCreateRequest(name, price, stockQuantity, description);
        return productService.create(request, mainImage);
    }

    @PutMapping(value = "/admin/products/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stockQuantity") Integer stockQuantity,
            @RequestParam("description") String description,
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage // 교체 시에만 첨부
    ) {
        ProductUpdateRequest req = new ProductUpdateRequest(name, price, stockQuantity, description, null, null);
        productService.updateWithImage(id, req, mainImage);
        return ResponseEntity.noContent().build();
    }

    //추가 이미지 API
    @PostMapping(value = "/admin/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Long> uploadImages(
            @PathVariable Long id,
            @RequestPart("files") List<MultipartFile> files
    ) throws Exception {
        return productService.uploadImages(id, files);
    }

    @PatchMapping("/admin/products/{id}/images/{imageId}/primary")
    public ResponseEntity<Void> setPrimary(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        productService.setPrimaryImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/products/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        productService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/admin/products/{id}")
    public void delete(@PathVariable Long id) { productService.deleteSoft(id); }

    @PostMapping("/admin/products/{id}/images")
    public List<Long> upload(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files) throws Exception {
        return productService.uploadImages(id, files);
    }
}