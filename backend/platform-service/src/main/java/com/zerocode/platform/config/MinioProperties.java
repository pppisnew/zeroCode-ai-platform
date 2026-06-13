package com.zerocode.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zerocode.minio")
public record MinioProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket) {

    public MinioProperties {
        if (endpoint == null || endpoint.isBlank()) {
            endpoint = "http://localhost:9000";
        }
        if (accessKey == null || accessKey.isBlank()) {
            accessKey = "zerocode";
        }
        if (secretKey == null || secretKey.isBlank()) {
            secretKey = "zerocode123";
        }
        if (bucket == null || bucket.isBlank()) {
            bucket = "zerocode";
        }
    }
}
