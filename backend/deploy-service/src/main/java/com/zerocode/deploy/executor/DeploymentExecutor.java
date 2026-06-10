package com.zerocode.deploy.executor;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import java.util.List;

public interface DeploymentExecutor {
    boolean supports(String target);

    DeploymentExecutionResult prepare(CreateDeploymentRequest request, List<String> plannedCommands);
}
