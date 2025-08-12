package com.ijin.hanaro.monitoring;

import org.springframework.boot.actuate.info.*;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AppInfoContributor implements InfoContributor {
    private final Environment env;
    public AppInfoContributor(Environment env) { this.env = env; }

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("app",
                Map.of(
                        "name", "hanaro-shopping",
                        "env", String.join(",", env.getActiveProfiles())
                )
        );
    }
}