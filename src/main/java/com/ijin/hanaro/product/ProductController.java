// product/ProductController.java
package com.ijin.hanaro.product;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

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
@Tag(name = "Products", description = "상품 공개 조회 및 관리자 상품 관리 API")
public class ProductController {
    private final ProductService productService;

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

    @Operation(summary = "상품 생성(관리자)", description = "멀티파트로 기본 정보 + 메인 이미지 업로드")
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

    @Operation(summary = "상품 수정(관리자)", description = "기본 정보 수정, 메인 이미지 교체 선택")
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
    @Operation(summary = "추가 이미지 업로드(관리자)", description = "여러 장 업로드 및 중복 체크(똑같은 이미지 중복 업로드시 에러처리")
    @PostMapping(value = "/admin/products/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public List<Long> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files
    ) throws Exception {
        return productService.uploadImages(id, files);
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