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
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepo;
    private final ProductImageRepository imageRepo;

    private static final Logger BIZ_LOG = LoggerFactory.getLogger("business.product");

    private static final int MAX_SINGLE = 512 * 1024;           // 512KB
    private static final long MAX_TOTAL_PER_PRODUCT = 3L * 1024 * 1024; // 3MB
    private static final Set<String> ALLOWED = Set.of("image/jpeg","image/png","image/webp");

    @Transactional
    public Long create(ProductCreateRequest r, MultipartFile mainImage) {
        if (mainImage == null || mainImage.isEmpty()) {
            throw new IllegalArgumentException("메인 이미지는 필수입니다.");
        }
        // 동일 이름 상품 중복 생성 방지 (소프트삭제되지 않은 상품만 검사)
        if (productRepo.existsByNameIgnoreCaseAndIsDeletedFalse(r.name())) {
            throw new IllegalStateException("이미 존재하는 상품명입니다: " + r.name());
        }

        if (!ALLOWED.contains(mainImage.getContentType())) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식 (JPEG, PNG, WEBP 만 가능)");
        }
        if (mainImage.getSize() > MAX_SINGLE) {
            throw new IllegalArgumentException("메인 이미지는 512KB를 초과할 수 없습니다.");
        }
        // 총합(대표 포함) 3MB 검사: 새 상품은 기존 사용량이 0이므로 메인만 검사하면 충분하지만, 규칙을 명확히 하기 위해 합산 체크
        long used = 0L;
        if (used + mainImage.getSize() > MAX_TOTAL_PER_PRODUCT) {
            throw new IllegalArgumentException("상품당 이미지 총합 3MB 초과");
        }

        Product p = new Product();
        p.setName(r.name());
        p.setDescription(r.description());
        p.setPrice(r.price());
        p.setStockQuantity(r.stockQuantity());
        // 우선 상품을 저장하여 ID를 확보
        p = productRepo.save(p);
        BIZ_LOG.info("PRODUCT_CREATE_BEGIN id={} name='{}' price={} stock={}", p.getId(), p.getName(), p.getPrice(), p.getStockQuantity());

        try {
            // 저장 경로: ./upload/YYYY/MM/DD
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
            try {
                mainImage.transferTo(target.toFile());
            } catch (NoSuchFileException | FileNotFoundException ex) {
                Files.copy(mainImage.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            }

            byte[] bytes = Files.readAllBytes(target);
            String checksum = sha256Hex(bytes);
            if (imageRepo.existsByChecksumSha256(checksum)) {
                throw new IllegalStateException("중복된 이미지입니다");
            }
            String storedPath = "/upload/%d/%02d/%02d".formatted(d.getYear(), d.getMonthValue(), d.getDayOfMonth());

            // ProductImage 엔티티로도 저장(대표 이미지)
            ProductImage img = new ProductImage();
            img.setProduct(p);
            img.setOriginalName(Objects.requireNonNull(mainImage.getOriginalFilename()));
            img.setStoredPath(storedPath);
            img.setStoredName(storedName);
            img.setSizeBytes(bytes.length);
            img.setChecksumSha256(checksum);
            img.setPrimaryImage(true);
            imageRepo.save(img);

            // 상품의 대표 경로도 동기화
            p.setMainImagePath(storedPath + "/" + storedName);
            productRepo.save(p);
            BIZ_LOG.info("PRODUCT_CREATED id={} name='{}' mainImagePath={} checksum={} bytes={}",
                    p.getId(), p.getName(), p.getMainImagePath(), checksum, bytes.length);
        } catch (IOException e) {
            BIZ_LOG.error("PRODUCT_CREATE_FAILED_IO name='{}' message={}", r.name(), e.getMessage(), e);
            throw new IllegalStateException("이미지 저장 중 오류가 발생했습니다.", e);
        } catch (Exception e) {
            BIZ_LOG.error("PRODUCT_CREATE_FAILED name='{}' message={}", r.name(), e.getMessage(), e);
            throw new RuntimeException(e);
        }

        return p.getId();
    }

    @Transactional
    public void update(Long id, ProductUpdateRequest r) {
        BIZ_LOG.info("PRODUCT_UPDATE_BEGIN id={}", id);
        Product p = productRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("상품 없음"));
        // 이름이 변경되는 경우에만 중복 검사
        if (r.name() != null && !p.getName().equalsIgnoreCase(r.name())) {
            if (productRepo.existsByNameIgnoreCaseAndIsDeletedFalseAndIdNot(r.name(), id)) {
                throw new IllegalStateException("이미 존재하는 상품명입니다: " + r.name());
            }
        }
        if (r.name() != null) p.setName(r.name());
        if (r.description() != null) p.setDescription(r.description());
        if (r.price() != null) p.setPrice(r.price());
        if (r.stockQuantity() != null) p.setStockQuantity(r.stockQuantity());
        if (r.mainImagePath() != null) p.setMainImagePath(r.mainImagePath());
        List<String> changed = new ArrayList<>();
        if (r.name() != null) changed.add("name");
        if (r.description() != null) changed.add("description");
        if (r.price() != null) changed.add("price");
        if (r.stockQuantity() != null) changed.add("stockQuantity");
        if (r.mainImagePath() != null) changed.add("mainImagePath");
        productRepo.save(p);
        BIZ_LOG.info("PRODUCT_UPDATED id={} changed={}", p.getId(), changed);
    }

    //상품삭제
    @Transactional
    public void deleteSoft(Long id) {
        BIZ_LOG.info("PRODUCT_DELETE_BEGIN id={}", id);
        Product p = productRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("상품 없음"));

        // 1) 파일 삭제 (best-effort)
        List<ProductImage> images = imageRepo.findByProduct_Id(id);
        for (ProductImage img : images) {
            try {
                String rel = (img.getStoredPath() + "/" + img.getStoredName()).replaceFirst("^/", "");
                Path path = Paths.get("src/main/resources/static").resolve(rel).toAbsolutePath();
                Files.deleteIfExists(path);
            } catch (Exception ignore) { /* 파일이 없어도 무시 */ }
        }

        // 2) 이미지 엔티티 삭제
        imageRepo.deleteAll(images);

        // 3) 상품 엔티티 삭제
        productRepo.delete(p);
        BIZ_LOG.info("PRODUCT_DELETED id={} imagesDeleted={}", id, images.size());
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> list(String q, Pageable pageable) {
        Page<Product> page = (q==null || q.isBlank())
                ? productRepo.findByIsDeletedFalse(pageable)
                : productRepo.findByIsDeletedFalseAndNameContainingIgnoreCase(q, pageable);
        return page.map(p -> {
            List<ProductImage> imgs = imageRepo.findByProduct_Id(p.getId());
            List<String> paths = imgs.stream()
                    .sorted(Comparator.comparing(ProductImage::isPrimaryImage).reversed()
                            .thenComparing(ProductImage::getId))
                    .map(img -> img.getStoredPath() + "/" + img.getStoredName())
                    .toList();
            return new ProductResponse(
                    p.getId(), p.getName(), p.getDescription(), p.getPrice(),
                    p.getStockQuantity(), p.getMainImagePath(), paths
            );
        });
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
        BIZ_LOG.info("IMAGES_UPLOAD_BEGIN productId={} count={} incomingBytes={} usedBytes={}", productId, files.size(), incoming, used);
        if (used + incoming > MAX_TOTAL_PER_PRODUCT)
            throw new IllegalArgumentException("상품당 이미지 총합 3MB 초과");

        long uploadedBytes = 0L;

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
            // Read bytes FIRST to avoid temp-file move issues
            byte[] bytes = f.getBytes();
            String checksum = sha256Hex(bytes);
            if (imageRepo.existsByChecksumSha256(checksum)) {
                throw new IllegalStateException("중복된 이미지입니다");
            }

            String uuid = UUID.randomUUID().toString().replace("-", "");
            String storedName = uuid + ext;
            Path target = base.resolve(storedName);
            Files.createDirectories(target.getParent());
            // Write using NIO to ensure parent exists and avoid transferTo() pitfalls
            Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

            ProductImage img = new ProductImage();
            img.setProduct(product);
            img.setOriginalName(Objects.requireNonNull(f.getOriginalFilename()));
            img.setStoredPath("/upload/%d/%02d/%02d".formatted(
                    d.getYear(), d.getMonthValue(), d.getDayOfMonth()));
            img.setStoredName(storedName);
            img.setSizeBytes(bytes.length);
            img.setChecksumSha256(checksum);
            img.setPrimaryImage(setPrimaryIfEmpty);
            uploadedBytes += bytes.length;
            Long savedId = imageRepo.save(img).getId();
            ids.add(savedId);

            // 대표 이미지가 비어있다면 첫 업로드를 자동으로 대표로 설정
            if (setPrimaryIfEmpty && (product.getMainImagePath() == null || product.getMainImagePath().isBlank())) {
                product.setMainImagePath(img.getStoredPath() + "/" + img.getStoredName());
                productRepo.save(product);
            }

            setPrimaryIfEmpty = false;
        }
        BIZ_LOG.info("IMAGES_UPLOADED productId={} count={} bytes={} newTotalBytes={}",
                productId, ids.size(), uploadedBytes, used + incoming);
        return ids;
    }

    //상품정보수정
    @Transactional
    public void updateWithImage(Long id, ProductUpdateRequest r, MultipartFile mainImage) {
        BIZ_LOG.info("PRODUCT_UPDATE_WITH_IMAGE_BEGIN id={} hasMainImage={}", id, (mainImage != null && !mainImage.isEmpty()));
        Product p = productRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("상품 없음"));
        // 이름이 변경되는 경우에만 중복 검사
        if (r.name() != null && !p.getName().equalsIgnoreCase(r.name())) {
            if (productRepo.existsByNameIgnoreCaseAndIsDeletedFalseAndIdNot(r.name(), id)) {
                throw new IllegalStateException("이미 존재하는 상품명입니다: " + r.name());
            }
        }
        if (r.name() != null) p.setName(r.name());
        if (r.description() != null) p.setDescription(r.description());
        if (r.price() != null) p.setPrice(r.price());
        if (r.stockQuantity() != null) p.setStockQuantity(r.stockQuantity());

        // 메인 이미지 교체가 들어온 경우에만 저장/검증
        if (mainImage != null && !mainImage.isEmpty()) {
            if (!ALLOWED.contains(mainImage.getContentType())) {
                throw new IllegalArgumentException("허용되지 않는 파일 형식 (JPEG, PNG, WEBP 만 가능)");
            }
            if (mainImage.getSize() > MAX_SINGLE) {
                throw new IllegalArgumentException("메인 이미지는 512KB를 초과할 수 없습니다.");
            }

            try {
                // 총합(대표 포함) 3MB 제한을 위해 기존 대표 이미지 용량을 제외하고 신규 용량을 더해 검사
                long used = imageRepo.sumSizeByProductId(id);
                Optional<ProductImage> oldPrimaryOpt = imageRepo.findByProduct_Id(id).stream()
                        .filter(ProductImage::isPrimaryImage)
                        .findFirst();
                long usedWithoutOldPrimary = used - oldPrimaryOpt.map(ProductImage::getSizeBytes).orElse(0);
                if (usedWithoutOldPrimary + mainImage.getSize() > MAX_TOTAL_PER_PRODUCT) {
                    throw new IllegalArgumentException("상품당 이미지 총합 3MB 초과");
                }

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

                byte[] bytes = Files.readAllBytes(target);
                String checksum = sha256Hex(bytes);
                Long oldPrimaryId = oldPrimaryOpt.map(ProductImage::getId).orElse(null);
                if (oldPrimaryId == null) {
                    if (imageRepo.existsByChecksumSha256(checksum)) {
                        throw new IllegalStateException("중복된 이미지입니다");
                    }
                } else {
                    if (imageRepo.existsByChecksumSha256AndIdNot(checksum, oldPrimaryId)) {
                        throw new IllegalStateException("중복된 이미지입니다");
                    }
                }
                String storedPath = "/upload/%d/%02d/%02d".formatted(d.getYear(), d.getMonthValue(), d.getDayOfMonth());

                // 기존 대표 이미지 있으면 파일/엔티티 삭제
                if (oldPrimaryOpt.isPresent()) {
                    ProductImage old = oldPrimaryOpt.get();
                    try {
                        String rel = (old.getStoredPath() + "/" + old.getStoredName()).replaceFirst("^/", "");
                        Path oldPath = Paths.get("src/main/resources/static").resolve(rel).toAbsolutePath();
                        Files.deleteIfExists(oldPath);
                    } catch (Exception ignore) { /* 파일이 없어도 무시 */ }
                    imageRepo.delete(old);
                }

                // 새 대표 이미지 저장
                ProductImage img = new ProductImage();
                img.setProduct(p);
                img.setOriginalName(Objects.requireNonNull(mainImage.getOriginalFilename()));
                img.setStoredPath(storedPath);
                img.setStoredName(storedName);
                img.setSizeBytes(bytes.length);
                img.setChecksumSha256(checksum);
                img.setPrimaryImage(true);
                imageRepo.save(img);

                // 상품 메인 경로 반영
                p.setMainImagePath(storedPath + "/" + storedName);
                BIZ_LOG.info("PRODUCT_PRIMARY_REPLACED id={} newPath={} checksum={} bytes={}",
                        p.getId(), p.getMainImagePath(), checksum, bytes.length);
            } catch (IOException e) {
                BIZ_LOG.error("PRODUCT_PRIMARY_REPLACE_FAILED_IO id={} message={}", id, e.getMessage(), e);
                throw new IllegalStateException("이미지 저장 중 오류가 발생했습니다.", e);
            } catch (Exception e) {
                BIZ_LOG.error("PRODUCT_PRIMARY_REPLACE_FAILED id={} message={}", id, e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }

        List<String> changed = new ArrayList<>();
        if (r.name() != null) changed.add("name");
        if (r.description() != null) changed.add("description");
        if (r.price() != null) changed.add("price");
        if (r.stockQuantity() != null) changed.add("stockQuantity");
        productRepo.save(p);
        BIZ_LOG.info("PRODUCT_UPDATED id={} changed={}", p.getId(), changed);
    }

    //추가 이미지 메소드

    // 이미지 교체 (대표 포함), 파일당 512KB, 상품 총합(대표 포함) 3MB
    @Transactional
    public void replaceImage(Long productId, Long imageId, MultipartFile file) throws Exception {
        BIZ_LOG.info("IMAGE_REPLACE_BEGIN productId={} imageId={}", productId, imageId);
        ProductImage img = imageRepo.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지 없음"));
        if (!Objects.equals(img.getProduct().getId(), productId)) {
            throw new IllegalArgumentException("상품-이미지 불일치");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("업로드할 파일이 없습니다");
        }
        if (!ALLOWED.contains(file.getContentType())) {
            throw new IllegalArgumentException("허용되지 않는 파일 형식 (JPEG, PNG, WEBP 만 가능)");
        }
        if (file.getSize() > MAX_SINGLE) {
            throw new IllegalArgumentException("파일 하나당 512KB 초과");
        }

        // 총합 3MB 제한: (현재 총합 - 교체 대상 용량 + 새 파일 용량) ≤ 3MB
        long used = imageRepo.sumSizeByProductId(productId);
        long usedWithoutThis = used - img.getSizeBytes();
        if (usedWithoutThis + file.getSize() > MAX_TOTAL_PER_PRODUCT) {
            throw new IllegalArgumentException("상품당 이미지 총합 3MB 초과");
        }

        // 저장 경로: ./upload/YYYY/MM/DD
        LocalDate d = LocalDate.now();
        Path base = Paths.get("src/main/resources/static/upload",
                String.valueOf(d.getYear()),
                String.format("%02d", d.getMonthValue()),
                String.format("%02d", d.getDayOfMonth()))
                .toAbsolutePath();
        Files.createDirectories(base);

        String ext = switch (Objects.requireNonNull(file.getContentType())) {
            case "image/jpeg" -> ".jpg";
            case "image/png"  -> ".png";
            case "image/webp" -> ".webp";
            default -> "";
        };
        byte[] bytes = file.getBytes();
        String checksum = sha256Hex(bytes);
        if (imageRepo.existsByChecksumSha256AndIdNot(checksum, imageId)) {
            throw new IllegalStateException("중복된 이미지입니다");
        }
        String storedName = UUID.randomUUID().toString().replace("-", "") + ext;
        Path target = base.resolve(storedName);
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

        // 기존 파일 삭제(best-effort)
        try {
            String rel = (img.getStoredPath() + "/" + img.getStoredName()).replaceFirst("^/", "");
            Path old = Paths.get("src/main/resources/static").resolve(rel).toAbsolutePath();
            Files.deleteIfExists(old);
        } catch (Exception ignore) { /* 파일이 없어도 무시 */ }

        // 메타데이터 갱신
        img.setOriginalName(Objects.requireNonNull(file.getOriginalFilename()));
        img.setStoredPath("/upload/%d/%02d/%02d".formatted(d.getYear(), d.getMonthValue(), d.getDayOfMonth()));
        img.setStoredName(storedName);
        img.setSizeBytes(bytes.length);
        img.setChecksumSha256(checksum);
        imageRepo.save(img);

        // 대표 이미지였다면 상품 메인 경로도 최신으로 동기화
        if (img.isPrimaryImage()) {
            Product p = img.getProduct();
            p.setMainImagePath(img.getStoredPath() + "/" + img.getStoredName());
            productRepo.save(p);
        }
        BIZ_LOG.info("IMAGE_REPLACED productId={} imageId={} checksum={} bytes={}",
                productId, imageId, checksum, bytes.length);
    }

    @Transactional
    public void setPrimaryImage(Long productId, Long imageId) {
        BIZ_LOG.info("PRIMARY_SET_BEGIN productId={} imageId={}", productId, imageId);
        ProductImage img = imageRepo.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("이미지 없음"));
        if (!Objects.equals(img.getProduct().getId(), productId)) {
            throw new IllegalArgumentException("상품-이미지 불일치");
        }
        Long prevPrimaryId = imageRepo.findByProduct_Id(productId).stream()
                .filter(ProductImage::isPrimaryImage)
                .map(ProductImage::getId)
                .findFirst()
                .orElse(null);
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
        BIZ_LOG.info("PRIMARY_SET productId={} prev={} now={}", productId, prevPrimaryId, imageId);
    }

    @Transactional
    public void deleteImage(Long productId, Long imageId) {
        BIZ_LOG.info("IMAGE_DELETE_BEGIN productId={} imageId={}", productId, imageId);
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
        BIZ_LOG.info("IMAGE_DELETED productId={} imageId={} wasPrimary={}", productId, imageId, wasPrimary);
    }

    //관리자 재고조정
    @Transactional
    public StockAdjustResponse adjustStock(Long productId, int deltaQty) {
        BIZ_LOG.info("STOCK_ADJUST_BEGIN productId={} delta={}", productId, deltaQty);
        Product p = productRepo.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("상품이 존재하지 않습니다. id=" + productId));
        int before = p.getStockQuantity();
        int after = before + deltaQty;
        if (after < 0) {
            throw new IllegalArgumentException("재고가 음수가 될 수 없습니다. (현재:" + p.getStockQuantity() + ", 변경:" + deltaQty + ")");
        }

        p.setStockQuantity(after);
        productRepo.save(p);
        BIZ_LOG.info("STOCK_ADJUSTED productId={} before={} delta={} after={}", productId, before, deltaQty, after);
        return new StockAdjustResponse(p.getId(), after);
    }

    //공개용 목록(검색,페이징)
    @Transactional(readOnly = true)
    public Page<ProductListItemResponse> listPublic(String q, Pageable pageable) {
        return productRepo.searchPublic(q, pageable)
                .map(p -> new ProductListItemResponse(
                        p.getId(),
                        p.getName(),
                        p.getPrice(),
                        p.getMainImagePath()
                ));
    }

    // --- 공개용 상세
    @Transactional(readOnly = true)
    public ProductDetailResponse detailPublic(Long id) {
        Product p = productRepo.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new IllegalArgumentException("상품을 찾을 수 없습니다. id=" + id));

        List<String> imagePaths = imageRepo.findByProduct_IdOrderByIdAsc(id).stream()
                .map(img -> img.getStoredPath() + "/" + img.getStoredName())
                .toList();

        return new ProductDetailResponse(
                p.getId(),
                p.getName(),
                p.getPrice(),
                p.getDescription(),
                p.getStockQuantity(),
                p.getMainImagePath(),
                imagePaths
        );
    }

    private static String sha256Hex(byte[] bytes) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] dig = md.digest(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : dig) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}