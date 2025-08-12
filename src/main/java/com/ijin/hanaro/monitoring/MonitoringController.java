package com.ijin.hanaro.monitoring;

import io.micrometer.core.instrument.MeterRegistry;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.boot.actuate.beans.BeansEndpoint;
import org.springframework.boot.actuate.env.EnvironmentEndpoint;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.metrics.MetricsEndpoint;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/monitor")
public class MonitoringController {

    private final HealthEndpoint healthEndpoint;
    private final BeansEndpoint beansEndpoint;
    private final EnvironmentEndpoint environmentEndpoint;
    private final MetricsEndpoint metricsEndpoint;

    public MonitoringController(
            HealthEndpoint healthEndpoint,
            BeansEndpoint beansEndpoint,
            EnvironmentEndpoint environmentEndpoint,
            MetricsEndpoint metricsEndpoint
    ) {
        this.healthEndpoint = healthEndpoint;
        this.beansEndpoint = beansEndpoint;
        this.environmentEndpoint = environmentEndpoint;
        this.metricsEndpoint = metricsEndpoint;
    }

    @Operation(
            summary = "애플리케이션 헬스 체크",
            description = "Spring Boot Actuator의 HealthEndpoint 결과를 반환합니다. 애플리케이션 상태와 각 컴포넌트의 health 정보를 확인할 수 있습니다."
    )
    @GetMapping("/health")
    public Object health() {
        return healthEndpoint.health();
    }

    @Operation(
            summary = "스프링 빈 목록 조회",
            description = "Actuator의 BeansEndpoint를 사용하여 애플리케이션에 등록된 빈과 의존관계를 반환합니다."
    )
    @GetMapping("/beans")
    public Object beans() {
        return beansEndpoint.beans();
    }

    @Operation(
            summary = "환경 변수/프로퍼티 조회",
            description = "Actuator의 EnvironmentEndpoint를 통해 활성 프로퍼티 소스와 환경 정보를 반환합니다."
    )
    @GetMapping("/env")
    public Object env() {
        return environmentEndpoint.environment(null);
    }

    @Operation(
            summary = "수집된 메트릭 이름 목록",
            description = "Actuator MetricsEndpoint의 listNames 결과에서 메트릭 이름들만 추출하여 반환합니다."
    )
    @GetMapping("/metrics")
    public Set<String> listMetrics() {
        return metricsEndpoint.listNames().getNames();
    }

}