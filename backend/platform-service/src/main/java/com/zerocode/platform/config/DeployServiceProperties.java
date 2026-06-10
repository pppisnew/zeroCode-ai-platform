package com.zerocode.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zerocode.deploy-service")
public record DeployServiceProperties(
        String baseUrl,
        String artifactBaseUrl) {

    public DeployServiceProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = "http://localhost:8081";
        }
        if (artifactBaseUrl == null || artifactBaseUrl.isBlank()) {
            artifactBaseUrl = "http://localhost:8080/api";
        }
        baseUrl = trimTrailingSlash(baseUrl);
        artifactBaseUrl = trimTrailingSlash(artifactBaseUrl);
    }

    public String artifactUrl(Long appId, Integer versionNo) {
        return artifactBaseUrl + "/apps/" + appId + "/versions/" + versionNo + "/zip";
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }
}
