# Hanaro 쇼핑몰 백엔드

간단한 쇼핑몰 백엔드 서비스.  
상품(이미지 업로드 포함), 주문, 장바구니, 통계 배치, Actuator 모니터링, 비즈니스 로그, `data.sql` export 및 테스트 적재를 지원합니다.

---

## 실행 방법

```bash
# 로컬 실행 (local 프로필)
./gradlew bootRun --args='--spring.profiles.active=local'
# 또는
./gradlew clean build
java -jar build/libs/hanaro-*.jar --spring.profiles.active=local
```


- 기본 포트: 8080
- Swagger UI: http://localhost:8080/swagger-ui/index.html

## 배치 작업, 스케줄러, Actuator, 로그 기록 확인 방법

### 1) 배치 작업(매출 통계) — 동작 방식
- **실행 시점**: 매일 00:10 (KST)
- **대상 주문**: 배송완료(DELIVERED) 기준으로 매출 집계
- **처리 로직(멱등성 보장)**
  1. 대상 일자에 대해 `daily_sales`, `daily_product_sales` 선삭제
  2. 주문/주문아이템에서 집계하여 재삽입
- **저장 테이블**
  - `daily_sales(sales_date, total_orders, total_items, total_amount)`
  - `daily_product_sales(sales_date, product_id, qty, amount)`

#### 수동 실행(로컬 디버그)
- 특정 일자 집계
  ```bash
  curl -X POST 'http://localhost:8080/admin/debug/aggregate?date=2025-08-11'
  ```
- 시드 생성(기간) + 일자별 즉시 집계
  ```bash
  curl -X POST 'http://localhost:8080/admin/debug/seed' \
    -d 'from=2025-08-09' -d 'to=2025-08-11' \
    -d 'username=sample_user' -d 'count=5' -d 'maxItemsPerOrder=3'
  ```
- 단일 일자 시드 + 즉시 집계
  ```bash
  curl -X POST 'http://localhost:8080/admin/debug/seed-and-aggregate' \
    -d 'date=2025-08-11' -d 'username=sample_user' \
    -d 'count=5' -d 'maxItemsPerOrder=3'
  ```

#### 결과 검증 체크리스트
- **로그**: `logs/business_order.log` (또는 날짜 디렉토리)
  ```
  [Seed] order id=... date=...
  BATCH_DAILY_SALES_BEGIN date=...
  BATCH_DAILY_SALES_DONE date=... orders=... items=... amount=... tookMs=...
  ```
- **DB**
  ```sql
  -- 집계 테이블 확인
  SELECT * FROM daily_sales WHERE sales_date = '2025-08-11';
  SELECT * FROM daily_product_sales WHERE sales_date = '2025-08-11' ORDER BY amount DESC;

  -- 원본(참고): 해당 일자의 DELIVERED 주문/아이템
  SELECT id, status, total_price FROM orders 
  WHERE DATE(created_at) = '2025-08-11' AND status='DELIVERED';
  ```

---

### 2) 스케줄러(주문 상태 전환) — 확인 방법
- **구성**: `@EnableScheduling` + 스케줄 클래스(예: `OrderStatusScheduler`)
- **주기**: 10분 간격 실행 (로그 타임스탬프 기준)
- **동작**: 상태 전환 예시 — `ORDERED → PREPARING`, `PREPARING → SHIPPING`
- **로그 확인**: `logs/business_order.log`
  ```
  [Scheduler] ORDERED -> PREPARING 변경 건수: N
  [Scheduler] PREPARING -> SHIPPING 변경 건수: N
  ```
- **DB 확인**
  ```sql
  SELECT id, status, status_updated_at 
  FROM orders 
  ORDER BY id DESC 
  LIMIT 20;
  ```
- **Tip**: 테스트 시 상태/시간 조건을 만족하도록 일부 주문의 `status`/`status_updated_at` 값을 조정한 뒤 스케줄러 실행 주기를 기다리면 변화를 확인할 수 있습니다.

---

### 3) Actuator — 사용법
- 기본 경로: `/actuator`
- 주요 엔드포인트
  - `/actuator/health` : 애플리케이션 상태
  - `/actuator/metrics` : 메트릭 목록
  - `/actuator/metrics/http.server.requests` : HTTP 요청 메트릭
  - `/actuator/env` : 환경 변수/프로퍼티
  - `/actuator/beans` : 빈 목록
- 메트릭 예시 조회
  ```
  GET /actuator/metrics/http.server.requests
  GET /actuator/metrics/http.server.requests?tag=uri:/admin/products&tag=method:GET
  ```


---

### 4) 로그 기록 — 구조/확인 방법
- **저장 위치**: `logs/` (실시간 파일 + 날짜별 롤링 디렉토리)
- **비즈니스 로거**
  - `business.order` → `business_order.log` (주문/배치)
  - `business.product` → `business_product.log` (상품/이미지)
- **파일 구조 예**
  ```
  logs/
    2025-08-12/
      business_order.0.log
      business_product.0.log
    business_order.log
    business_product.log
  ```
- **실시간 확인**
  ```bash
  tail -f logs/business_order.log
  tail -f logs/business_product.log
  ```
- **대표 메시지 예시**
  ```
  ORDER_CREATE_BEGIN username=...
  ORDER_CREATED id=.. orderNo=.. total=..
  ORDER_ITEM_ADDED orderId=.. productId=.. qty=..
  BATCH_DAILY_SALES_BEGIN date=..
  BATCH_DAILY_SALES_DONE date=.. orders=.. items=.. amount=.. tookMs=..

  PRODUCT_CREATE id=.. name='..' price=..
  PRODUCT_UPDATE id=.. changed=[price, stockQuantity]
  PRODUCT_IMAGE_UPSERT productId=.. primary=true count=..
  ```
 
## Admin Sample Data Controller (local)

> 로컬 개발 편의용 디버그 엔드포인트입니다. 보안상 운영/스테이징에서는 비활성화/차단

### 1) POST /admin/debug/seed
- **설명**: 매출 통계 테스트용 샘플 주문/아이템을 기간 단위로 생성하고, 각 일자별 통계를 즉시 집계합니다.
- **폼 파라미터(form-data or x-www-form-urlencoded)**
  - `from`(yyyy-MM-dd)
  - `to`(yyyy-MM-dd)
  - `username`(샘플 주문 생성 사용자명)
  - `count`(일자당 주문 수, 기본 5)
  - `maxItemsPerOrder`(주문당 최대 아이템 수, 기본 3)
- **예시**
  ```bash
  curl -X POST 'http://localhost:8080/admin/debug/seed' \
    -d 'from=2025-08-09' -d 'to=2025-08-11' \
    -d 'username=sample_user' -d 'count=5' -d 'maxItemsPerOrder=3'
  ```

### 2) POST /admin/debug/export-data
- **설명**: 현재 DB 데이터를 `src/main/resources/data/data.sql`로 Export합니다. (제출 직전 스냅샷 생성)
- **주의**: FK 무결성 보장을 위해 `FOREIGN_KEY_CHECKS`를 끄고 TRUNCATE/INSERT 순으로 생성됩니다.
- **예시**
  ```bash
  curl -X POST 'http://localhost:8080/admin/debug/export-data'
  ```

### 3) POST /admin/debug/aggregate
- **설명**: 특정 일자의 매출 통계를 수동 집계합니다. (스케줄러 우회)
- **쿼리 파라미터**
  - `date`(yyyy-MM-dd)
- **예시**
  ```bash
  curl -X POST 'http://localhost:8080/admin/debug/aggregate?date=2025-08-11'
  ```

> 로그는 `logs/business_order.log` 에 기록되며, 집계 완료 시 `BATCH_DAILY_SALES_DONE` 메시지로 확인할 수 있습니다.

### 기술 스택
- Java 21, Spring Boot 3.3.x
- Spring Web / Security / Validation / Data JPA
- MySQL 8.x
- Lombok, Jakarta Validation
- Springdoc(OpenAPI) + Swagger UI
- Actuator (관찰/모니터링)
- Logback (롤링 파일, 비즈니스 로그 분리)
  
### 디렉토리 구조
  ```
.
├─ logs/
├─ src/
│  ├─ main/
│  │  ├─ java/
│  │  │  └─ com/
│  │  │     └─ ijin/
│  │  │        └─ hanaro/
│  │  │           ├─ auth/
│  │  │           ├─ cart/
│  │  │           ├─ common/
│  │  │           ├─ config/
│  │  │           ├─ monitoring/
│  │  │           ├─ order/
│  │  │           ├─ product/
│  │  │           ├─ stats/
│  │  │           ├─ user/
│  │  │           └─ HanaroApplication.java
│  │  └─ resources/
│  │     ├─ application.yml
│  │     ├─ application-local.yml
│  │     ├─ data/
│  │     │  └─ data.sql
│  │     └─ static/
│  │        └─ upload/
└─ README.md

  ```
