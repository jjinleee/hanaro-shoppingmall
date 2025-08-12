## 배치 작업, 스케줄러, Actuator, 로그 기록 확인 방법

### 1. 배치 작업 결과 확인
- 매일 00:10(KST) 실행 → `logs/business_*.log`에 기록됨
    - 예: `logs/business_order.log`, `logs/business_product.log`
- 매출 통계 → DB 테이블 `daily_sales`, `daily_product_sales` 확인

### 2. 스케줄러 확인
- 스케줄러는 Spring `@Scheduled` 기반으로 동작
- 즉시 실행 테스트:
    - Swagger → `POST /admin/debug/aggregate?date=YYYY-MM-DD`
    - 실행 후 로그와 DB에서 결과 확인

### 3. Actuator 확인
- 기본 경로: `/actuator`
- 주요 엔드포인트:
    - `/actuator/health` : 애플리케이션 상태
    - `/actuator/metrics` : 메트릭 정보
    - `/actuator/env` : 환경 정보
    - `/actuator/beans` : 빈 목록
- 운영 환경에서는 민감 엔드포인트는 비활성화 권장

### 4. 로그 기록 확인
- 모든 주요 비즈니스 이벤트 로그는 날짜별 디렉토리로 롤링 저장
- 로그 경로 예: 아래와 같이 날짜별 디렉토리로 저장되며 최신 파일과 롤링 파일이 함께 존재함
  ```
  logs/
    2025-08-12/
      business_order.0.log
      business_product.0.log
    business_order.log
    business_product.log
  ```
- 상품 관련 액션(등록, 수정, 삭제) → `business_product.log`
- 주문 및 배치 관련 액션 → `business_order.log`