package com.ijin.hanaro.product;

import com.ijin.hanaro.product.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepo;
    private final ProductImageRepository imageRepo;

    private static final int MAX_SINGLE = 512 * 1024;           // 512KB
    private static final long MAX_TOTAL_PER_PRODUCT = 3L * 1024 * 1024; // 3MB
    private static final Set<String> ALLOWED = Set.of("image/jpeg","image/png","image/webp");

    @Transactional
    public Long create(ProductCreateRequest r, MultipartFile mainImage) {
        if (mainImage == null || mainImage.isEmpty()) {
            throw new IllegalArgumentException("메인 이미지는 필수입니다.");
        }

        Product p = new Product();
        p.setName(r.name());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setStockQuantity(r.stockQuantity());

        if (!ALLOWED.contains(mainImage.getContentType())) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식 (JPEG, PNG, WEBP 만 가능)");
        }
        if (mainImage.getSize() > MAX_SINGLE) {
            throw new IllegalArgumentException("메인 이미지는 512KB를 초과할 수 없습니다.");
        }

        try {
            // 저장 경로: ./uploads/YYYY/MM/DD
            LocalDate d = LocalDate.now();
            Path base = Paths.get("src/main/resources/static/upload",
                    String.valueOf(d.getYear()),
                    String.format("%02d", d.getMonthValue()),
                    String.format("%02d", d.getDayOfMonth()))
                    .toAbsolutePath();
            Files.createDirectories(base);

            String ext = switch (Objects.requireNonNull(mainImage.getContentType())) {
                case "image/jpeg" -> ".jpg";
                case "image/png"  -> ".png";
                case "image/webp" -> ".webp";
                default -> "";
            };
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String storedName = uuid + ext;
            Path target = base.resolve(storedName);
            mainImage.transferTo(target.toFile());

            String imagePath = "/upload/%d/%02d/%02d/%s".formatted(
                    d.getYear(), d.getMonthValue(), d.getDayOfMonth(), storedName);
            p.setMainImagePath(imagePath);
        } catch (IOException e) {
            throw new IllegalStateException("이미지 저장 중 오류가 발생했습니다.", e);
        }

        return productRepo.save(p).getId();
    }

    @Transactional
    public void update(Long id, ProductUpdateRequest r) {
        Product p = productRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("상품 없음"));
        p.setName(r.name());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setStockQuantity(r.stockQuantity());
        if (r.mainImagePath()!=null) p.setMainImagePath(r.mainImagePath());
        if (r.deleted()!=null) p.setDeleted(r.deleted());
        productRepo.save(p);
    }

    @Transactional
    public void deleteSoft(Long id) {
        Product p = productRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("상품 없음"));
        p.setDeleted(true);
        productRepo.save(p);
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(String q, Pageable pageable) {
        Page<Product> page = (q==null || q.isBlank())
                ? productRepo.findByIsDeletedFalse(pageable)
                : productRepo.findByIsDeletedFalseAndNameContainingIgnoreCase(q, pageable);
        return page.map(p -> new ProductResponse(
                p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStockQuantity(), p.getMainImagePath(), List.of()
        ));
    }

    @Transactional(readOnly = true)
    public ProductResponse detail(Long id) {
        Product p = productRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("상품 없음"));
        if (p.isDeleted()) throw new IllegalArgumentException("삭제된 상품입니다");
        List<String> paths = p.getImages().stream()
                .sorted(Comparator.comparing(ProductImage::isPrimaryImage).reversed())
                .map(img -> img.getStoredPath() + "/" + img.getStoredName())
                .toList();
        return new ProductResponse(p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                p.getStockQuantity(), p.getMainImagePath(), paths);
    }

    /** 여러 장 업로드: 단일 ≤512KB, 상품 총합 ≤3MB, 최대 5장 권장 */
    @Transactional
    public List<Long> uploadImages(Long productId, List<MultipartFile> files) throws Exception {
        Product product = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품 없음"));

        // 개별 용량/타입 체크
        for (MultipartFile f : files) {
            if (f.isEmpty()) throw new IllegalArgumentException("빈 파일 포함");
            if (!ALLOWED.contains(f.getContentType())) throw new IllegalArgumentException("허용되지 않는 파일 형식");
            if (f.getSize() > MAX_SINGLE) throw new IllegalArgumentException("파일 하나당 512KB 초과");
        }
        // 총합 체크
        long used = imageRepo.sumSizeByProductId(productId);
        long incoming = files.stream().mapToLong(MultipartFile::getSize).sum();
        if (used + incoming > MAX_TOTAL_PER_PRODUCT)
            throw new IllegalArgumentException("상품당 이미지 총합 3MB 초과");

        // 저장 경로: ./uploads/YYYY/MM/DD
        LocalDate d = LocalDate.now();
        Path base = Paths.get("src/main/resources/static/upload",
                String.valueOf(d.getYear()),
                String.format("%02d", d.getMonthValue()),
                String.format("%02d", d.getDayOfMonth()))
                .toAbsolutePath();
        Files.createDirectories(base);

        boolean setPrimaryIfEmpty = imageRepo.countByProduct_Id(productId) == 0;

        List<Long> ids = new ArrayList<>();
        for (MultipartFile f : files) {
            String ext = switch (Objects.requireNonNull(f.getContentType())) {
                case "image/jpeg" -> ".jpg";
                case "image/png"  -> ".png";
                case "image/webp" -> ".webp";
                default -> "";
            };
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String storedName = uuid + ext;
            Path target = base.resolve(storedName);
            f.transferTo(target.toFile());

            ProductImage img = new ProductImage();
            img.setProduct(product);
            img.setOriginalName(Objects.requireNonNull(f.getOriginalFilename()));
            img.setStoredPath("/upload/%d/%02d/%02d".formatted(
                    d.getYear(), d.getMonthValue(), d.getDayOfMonth()));
            img.setStoredName(storedName);
            img.setSizeBytes((int) f.getSize());
            img.setChecksumSha256(sha256Hex(f.getBytes()));
            img.setPrimaryImage(setPrimaryIfEmpty);
            Long savedId = imageRepo.save(img).getId();
            ids.add(savedId);

            // 대표 이미지가 비어있다면 첫 업로드를 자동으로 대표로 설정
            if (setPrimaryIfEmpty && (product.getMainImagePath() == null || product.getMainImagePath().isBlank())) {
                product.setMainImagePath(img.getStoredPath() + "/" + img.getStoredName());
                productRepo.save(product);
            }

            setPrimaryIfEmpty = false;
        }
        return ids;


    }

    //상품정보수정
    @Transactional
    public void updateWithImage(Long id, ProductUpdateRequest r, MultipartFile mainImage) {
        Product p = productRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("상품 없음"));
        p.setName(r.name());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setStockQuantity(r.stockQuantity());

        // 메인 이미지 교체가 들어온 경우에만 저장/검증
        if (mainImage != null && !mainImage.isEmpty()) {
            if (!ALLOWED.contains(mainImage.getContentType())) {
                throw new IllegalArgumentException("허용되지 않는 파일 형식 (JPEG, PNG, WEBP 만 가능)");
            }
            if (mainImage.getSize() > MAX_SINGLE) {
                throw new IllegalArgumentException("메인 이미지는 512KB를 초과할 수 없습니다.");
            }

            try {
                LocalDate d = LocalDate.now();
                Path base = Paths.get("src/main/resources/static/upload",
                                String.valueOf(d.getYear()),
                                String.format("%02d", d.getMonthValue()),
                                String.format("%02d", d.getDayOfMonth()))
                        .toAbsolutePath();
                Files.createDirectories(base);

                String ext = switch (Objects.requireNonNull(mainImage.getContentType())) {
                    case "image/jpeg" -> ".jpg";
                    case "image/png"  -> ".png";
                    case "image/webp" -> ".webp";
                    default -> "";
                };
                String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
                Path target = base.resolve(storedName);
                mainImage.transferTo(target.toFile());

                String imagePath = "/upload/%d/%02d/%02d/%s".formatted(
                        d.getYear(), d.getMonthValue(), d.getDayOfMonth(), storedName);
                p.setMainImagePath(imagePath);
            } catch (IOException e) {
                throw new IllegalStateException("이미지 저장 중 오류가 발생했습니다.", e);
            }
        }

        productRepo.save(p);
    }

    //추가 이미지 메소드

    @Transactional
    public void setPrimaryImage(Long productId, Long imageId) {
        ProductImage img = imageRepo.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지 없음"));
        if (!Objects.equals(img.getProduct().getId(), productId)) {
            throw new IllegalArgumentException("상품-이미지 불일치");
        }
        // 기존 대표 해제
        imageRepo.findByProduct_Id(productId).forEach(i -> {
            if (i.isPrimaryImage()) i.setPrimaryImage(false);
        });
        img.setPrimaryImage(true);
        imageRepo.save(img);

        // 상품 메인 경로 동기화
        Product p = img.getProduct();
        p.setMainImagePath(img.getStoredPath() + "/" + img.getStoredName());
        productRepo.save(p);
    }

    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        ProductImage img = imageRepo.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지 없음"));
        if (!Objects.equals(img.getProduct().getId(), productId)) {
            throw new IllegalArgumentException("상품-이미지 불일치");
        }
        boolean wasPrimary = img.isPrimaryImage();
        imageRepo.delete(img);

        // 대표를 지웠다면 다른 이미지 하나를 대표로 승격 + 상품 메인 갱신
        if (wasPrimary) {
            imageRepo.findByProduct_Id(productId).stream().findFirst().ifPresent(next -> {
                next.setPrimaryImage(true);
                imageRepo.save(next);
                Product p = next.getProduct();
                p.setMainImagePath(next.getStoredPath() + "/" + next.getStoredName());
                productRepo.save(p);
            });
        }
    }



    private static String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] dig = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}