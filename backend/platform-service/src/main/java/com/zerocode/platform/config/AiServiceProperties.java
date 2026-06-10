package com.zerocode.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zerocode.ai-service")
public record AiServiceProperties(String baseUrl) {
}
