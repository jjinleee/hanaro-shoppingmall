// product/ProductController.java
package com.ijin.hanaro.product;

import com.ijin.hanaro.product.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class ProductController {
    private final ProductService service;

    // ===== 공개 조회 =====
    @GetMapping("/products")
    public Page<ProductResponse> list(@RequestParam(required=false) String q,
                                      @RequestParam(defaultValue="0") int page,
                                      @RequestParam(defaultValue="10") int size) {
        return service.list(q, PageRequest.of(page, size, Sort.by("id").descending()));
    }

    @GetMapping("/products/{id}")
    public ProductResponse detail(@PathVariable Long id) { return service.detail(id); }

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
        return service.create(request, mainImage);
    }

    @PutMapping("/admin/products/{id}")
    public void update(@PathVariable Long id, @Valid @RequestBody ProductUpdateRequest r) { service.update(id, r); }

    @DeleteMapping("/admin/products/{id}")
    public void delete(@PathVariable Long id) { service.deleteSoft(id); }

    @PostMapping("/admin/products/{id}/images")
    public List<Long> upload(@PathVariable Long id, @RequestParam("files") List<MultipartFile> files) throws Exception {
        return service.uploadImages(id, files);
    }
}