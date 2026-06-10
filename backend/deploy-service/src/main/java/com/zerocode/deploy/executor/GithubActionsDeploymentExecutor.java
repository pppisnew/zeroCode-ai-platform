package com.zerocode.deploy.executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GithubActionsDeploymentExecutor extends TargetDeploymentExecutor {
    public GithubActionsDeploymentExecutor(
            @Value("${zerocode.deploy.executors.github-actions.enabled:false}") boolean enabled) {
        super("github-actions", enabled);
    }

    @Override
    protected String executorName() {
        return "GitHub Actions";
    }
}
