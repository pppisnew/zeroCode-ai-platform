package com.zerocode.deploy.executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DockerDeploymentExecutor extends TargetDeploymentExecutor {
    public DockerDeploymentExecutor(
            @Value("${zerocode.deploy.executors.docker.enabled:false}") boolean enabled) {
        super("docker", enabled);
    }

    @Override
    protected String executorName() {
        return "Docker";
    }
}
