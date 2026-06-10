package com.zerocode.platform.dto;

public record DeployServiceDeploymentRequest(
        Long appId,
        Integer versionNo,
        String projectType,
        String artifactUrl,
        String target) {
}
