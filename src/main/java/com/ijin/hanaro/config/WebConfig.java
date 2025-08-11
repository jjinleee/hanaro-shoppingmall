package com.ijin.hanaro.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload.root}")
    private String uploadRoot;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // 예: /upload/2025/08/11/abc.jpg  →  {uploadRoot}/2025/08/11/abc.jpg
        registry.addResourceHandler("/upload/**")
                .addResourceLocations("file:" + ensureTrailingSlash(uploadRoot),
                        "classpath:/static/upload/")
                .setCachePeriod(3600);
    }

    private String ensureTrailingSlash(String p) {
        return p.endsWith("/") ? p : p + "/";
    }
}