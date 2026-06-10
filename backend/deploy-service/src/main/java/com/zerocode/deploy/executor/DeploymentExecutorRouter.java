package com.zerocode.deploy.executor;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DeploymentExecutorRouter {
    private final List<DeploymentExecutor> executors;
    private final DryRunDeploymentExecutor dryRunDeploymentExecutor;

    public DeploymentExecutorRouter(
            List<DeploymentExecutor> executors,
            DryRunDeploymentExecutor dryRunDeploymentExecutor) {
        this.executors = executors;
        this.dryRunDeploymentExecutor = dryRunDeploymentExecutor;
    }

    public DeploymentExecutionResult prepare(
            CreateDeploymentRequest request,
            List<String> plannedCommands) {
        return executors.stream()
                .filter(executor -> executor != dryRunDeploymentExecutor)
                .filter(executor -> executor.supports(request.target()))
                .findFirst()
                .orElse(dryRunDeploymentExecutor)
                .prepare(request, plannedCommands);
    }
}
