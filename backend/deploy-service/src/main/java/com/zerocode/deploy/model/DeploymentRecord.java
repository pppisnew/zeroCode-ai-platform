package com.zerocode.deploy.model;

import java.time.LocalDateTime;
import java.util.List;

public record DeploymentRecord(
        String id,
        Long appId,
        Integer versionNo,
        String projectType,
        String artifactUrl,
        String target,
        String status,
        List<String> plannedCommands,
        List<String> executionLogs,
        String accessUrl,
        LocalDateTime createTime) {
}
