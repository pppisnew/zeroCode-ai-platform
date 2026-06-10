package com.zerocode.deploy.executor;

import java.util.List;

public record DeploymentExecutionResult(
        String status,
        List<String> executionLogs,
        String accessUrl) {
}
