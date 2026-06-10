package com.zerocode.deploy.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class DryRunDeploymentExecutorTests {
    private final DryRunDeploymentExecutor executor = new DryRunDeploymentExecutor();

    @Test
    void returnsPlannedResultWithoutExecutingCommands() {
        DeploymentExecutionResult result = executor.prepare(
                new CreateDeploymentRequest(
                        10L,
                        2,
                        "react",
                        "https://example.com/app.zip",
                        "kubernetes"),
                List.of("kubectl apply -f deployment.yaml"));

        assertThat(result.status()).isEqualTo("planned");
        assertThat(result.accessUrl()).isNull();
        assertThat(result.executionLogs())
                .containsExactly(
                        "Dry-run deployment: no command executed",
                        "Target: kubernetes",
                        "Artifact: https://example.com/app.zip",
                        "Planned: kubectl apply -f deployment.yaml");
    }
}
