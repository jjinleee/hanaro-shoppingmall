// product/ProductController.java
package com.ijin.hanaro.product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import com.ijin.hanaro.product.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.Set;
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
@Tag(name = "Products", description = "상품 공개 조회 및 관리자 상품 관리 API")
public class ProductController {
    private final ProductService productService;
    private final Validator validator;

    // ===== 공개 조회 =====
    @Operation(summary = "상품 목록 조회(공개)", description = "검색어 q(이름/설명 LIKE), 페이지네이션 page/size 지원")
    @GetMapping("/products")
    public Page<ProductListItemResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        return productService.listPublic(q, pageable);
    }

    /** 공개용 상품 상세 */
    @Operation(summary = "상품 상세 조회(공개)", description = "상품 상세 및 대표 이미지, 가격/재고 정보를 조회")
    @GetMapping("/products/{id}")
    public ProductDetailResponse detail(@PathVariable Long id) {
        return productService.detailPublic(id);
    }

    // ===== 관리자 전용 =====
    @Operation(summary = "상품 목록 조회(관리자)", description = "관리자 전용 검색/정렬/페이지네이션 목록")
    @GetMapping("/admin/products")
    public Page<ProductResponse> adminList(@RequestParam(required = false) String q,
                                           @RequestParam(defaultValue = "0") int page,
                                           @RequestParam(defaultValue = "10") int size) {
        return productService.list(q, PageRequest.of(page, size, Sort.by("id").descending()));
    }

    @Operation(summary = "상품 상세 조회(관리자)", description = "관리자 전용 상세")
    @GetMapping("/admin/products/{id}")
    public ProductResponse adminDetail(@PathVariable Long id) {
        return productService.detail(id);
    }

    @Operation(summary = "상품 생성(관리자)", description = "개별 폼 필드 + 이미지 여러 장(각 512KB 이하, 총합 3MB 이하 / 첫 장은 대표, 나머지는 추가 이미지)")
    @PostMapping(value = "/admin/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Long create(
            @RequestParam("name") String name,
            @RequestParam("price") java.math.BigDecimal price,
            @RequestParam("stockQuantity") Integer stockQuantity,
            @RequestParam(value = "description", required = false) String description,
            @RequestPart("images") java.util.List<org.springframework.web.multipart.MultipartFile> images
    ) throws Exception {
        if (images == null || images.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지가 없습니다");
        }
        ProductCreateRequest request = new ProductCreateRequest(name, price, stockQuantity, description);
        java.util.Set<jakarta.validation.ConstraintViolation<ProductCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new jakarta.validation.ConstraintViolationException(violations);
        }
        // 첫 번째 이미지를 대표 이미지로 사용하여 상품 생성
        org.springframework.web.multipart.MultipartFile mainImage = images.get(0);
        Long productId = productService.create(request, mainImage);
        // 나머지 이미지는 추가 이미지로 업로드
        if (images.size() > 1) {
            productService.uploadImages(productId, images.subList(1, images.size()));
        }
        return productId;
    }

    @Operation(
        summary = "상품 수정(관리자)",
        description = "개별 폼 필드(name, price, stockQuantity, description, mainImagePath) + mainImage(file)[선택] + images(여러 장, 각 512KB 이하, 총합 3MB 이하)"
    )
    @PutMapping(value = "/admin/products/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> update(
            @PathVariable Long id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam(value = "stockQuantity", required = false) Integer stockQuantity,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "mainImagePath", required = false) String mainImagePath,
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
            @RequestPart(value = "images", required = false) List<MultipartFile> images
    ) throws Exception {
        ProductUpdateRequest req = new ProductUpdateRequest(name, price, stockQuantity, description, mainImagePath);
        Set<ConstraintViolation<ProductUpdateRequest>> violations = validator.validate(req);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        productService.updateWithImage(id, req, mainImage);
        if (images != null && !images.isEmpty()) {
            productService.uploadImages(id, images);
        }
        return ResponseEntity.noContent().build();
    }

    //추가 이미지 API
    @Operation(summary = "추가 이미지 업로드(관리자)", description = "여러 장 업로드 및 중복 체크(각 512KB 이하, 상품 총합 3MB 이하)")
    @PostMapping(value = "/admin/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Long> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files
    ) throws Exception {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("업로드할 이미지가 없습니다");
        }
        return productService.uploadImages(id, files);
    }

    @Operation(summary = "상품 이미지 교체(관리자)", description = "기존 이미지 파일을 새 파일로 교체 (각 512KB 이하, 상품 총합 3MB 이하)")
    @PutMapping(value = "/admin/products/{id}/images/{imageId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> replaceImage(
            @PathVariable Long id,
            @PathVariable Long imageId,
            @RequestPart("file") MultipartFile file
    ) throws Exception {
        productService.replaceImage(id, imageId, file);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "대표 이미지 지정(관리자)", description = "해당 상품의 대표 이미지를 지정")
    @PatchMapping("/admin/products/{id}/images/{imageId}/primary")
    public ResponseEntity<Void> setPrimary(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        productService.setPrimaryImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "상품 이미지 삭제(관리자)", description = "이미지 단건 삭제")
    @DeleteMapping("/admin/products/{id}/images/{imageId}")
    public ResponseEntity<Void> deleteImage(
            @PathVariable Long id,
            @PathVariable Long imageId
    ) {
        productService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "상품 삭제(관리자)", description = "상품 삭제")
    @DeleteMapping("/admin/products/{id}")
    public void delete(@PathVariable Long id) { productService.deleteSoft(id); }

    //재고 조정
    @Operation(summary = "재고 수량 조정(관리자)", description = "증가/감소 deltaQty 반영")
    @PostMapping("/admin/products/{id}/stock-adjustments")
    public StockAdjustResponse adjustStock(@PathVariable Long id,
                                           @RequestBody @Valid StockAdjustRequest req) {
        return productService.adjustStock(id, req.deltaQty());
    }
}