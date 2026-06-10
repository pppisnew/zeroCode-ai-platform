package com.zerocode.deploy.executor;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KubernetesDeploymentExecutor extends TargetDeploymentExecutor {
    public KubernetesDeploymentExecutor(
            @Value("${zerocode.deploy.executors.kubernetes.enabled:false}") boolean enabled) {
        super("kubernetes", enabled);
    }

    @Override
    protected String executorName() {
        return "Kubernetes";
    }
}
