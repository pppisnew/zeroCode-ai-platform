package com.zerocode.deploy.executor;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import com.zerocode.deploy.model.DeploymentStatus;
import java.util.ArrayList;
import java.util.List;

abstract class TargetDeploymentExecutor implements DeploymentExecutor {
    private final String target;
    private final boolean enabled;

    TargetDeploymentExecutor(String target, boolean enabled) {
        this.target = target;
        this.enabled = enabled;
    }

    @Override
    public boolean supports(String target) {
        return enabled && this.target.equals(target);
    }

    @Override
    public DeploymentExecutionResult prepare(
            CreateDeploymentRequest request,
            List<String> plannedCommands) {
        List<String> logs = new ArrayList<>();
        logs.add(executorName() + " executor enabled");
        logs.add("Real command execution is not implemented in this build");
        logs.add("Target: " + request.target());
        logs.add("Artifact: " + request.artifactUrl());
        for (String command : plannedCommands) {
            logs.add("Skipped: " + command);
        }
        return new DeploymentExecutionResult(DeploymentStatus.SKIPPED.value(), logs, null);
    }

    protected abstract String executorName();
}
