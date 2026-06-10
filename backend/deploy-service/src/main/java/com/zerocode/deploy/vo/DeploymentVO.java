package com.zerocode.deploy.vo;

import java.time.LocalDateTime;
import java.util.List;

public record DeploymentVO(
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
