package com.zerocode.deploy.executor;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import com.zerocode.deploy.model.DeploymentStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DryRunDeploymentExecutor implements DeploymentExecutor {
    @Override
    public boolean supports(String target) {
        return true;
    }

    @Override
    public DeploymentExecutionResult prepare(
            CreateDeploymentRequest request,
            List<String> plannedCommands) {
        List<String> logs = new ArrayList<>();
        logs.add("Dry-run deployment: no command executed");
        logs.add("Target: " + request.target());
        logs.add("Artifact: " + request.artifactUrl());
        for (String command : plannedCommands) {
            logs.add("Planned: " + command);
        }
        return new DeploymentExecutionResult(DeploymentStatus.PLANNED.value(), logs, null);
    }
}
