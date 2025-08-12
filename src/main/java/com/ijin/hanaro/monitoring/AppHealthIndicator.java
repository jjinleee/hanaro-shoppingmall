package com.ijin.hanaro.monitoring;

import org.springframework.boot.actuate.health.*;
import org.springframework.stereotype.Component;

@Component
public class AppHealthIndicator implements HealthIndicator {
    @Override
    public Health health() {
        // 필요하면 내부 점검 로직 추가 (예: 파일 저장 루트 쓰기 가능 여부 등)
        return Health.up()
                .withDetail("version", "1.0.0")
                .withDetail("storage", "OK")
                .build();
    }
}