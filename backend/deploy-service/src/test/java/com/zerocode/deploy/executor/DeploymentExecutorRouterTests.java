package com.zerocode.deploy.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeploymentExecutorRouterTests {

    @Test
    void fallsBackToDryRunWhenTargetExecutorIsDisabled() {
        DryRunDeploymentExecutor dryRunExecutor = new DryRunDeploymentExecutor();
        DeploymentExecutorRouter router = new DeploymentExecutorRouter(
                List.of(new DockerDeploymentExecutor(false), dryRunExecutor),
                dryRunExecutor);

        DeploymentExecutionResult result = router.prepare(request("docker"), List.of("docker build ."));

        assertThat(result.status()).isEqualTo("planned");
        assertThat(result.executionLogs()).contains("Dry-run deployment: no command executed");
    }

    @Test
    void routesToEnabledTargetExecutor() {
        DryRunDeploymentExecutor dryRunExecutor = new DryRunDeploymentExecutor();
        DeploymentExecutorRouter router = new DeploymentExecutorRouter(
                List.of(new DockerDeploymentExecutor(true), dryRunExecutor),
                dryRunExecutor);

        DeploymentExecutionResult result = router.prepare(request("docker"), List.of("docker build ."));

        assertThat(result.status()).isEqualTo("skipped");
        assertThat(result.executionLogs())
                .contains(
                        "Docker executor enabled in dry-run mode",
                        "Set zerocode.deploy.executors.docker.execution-mode=real to execute Docker commands",
                        "Skipped: docker build .");
    }

    @Test
    void githubActionsAndKubernetesExecutorsRequireExplicitEnablement() {
        assertThat(new GithubActionsDeploymentExecutor(false).supports("github-actions")).isFalse();
        assertThat(new GithubActionsDeploymentExecutor(true).supports("github-actions")).isTrue();
        assertThat(new KubernetesDeploymentExecutor(false).supports("kubernetes")).isFalse();
        assertThat(new KubernetesDeploymentExecutor(true).supports("kubernetes")).isTrue();
    }

    private static CreateDeploymentRequest request(String target) {
        return new CreateDeploymentRequest(
                10L,
                2,
                "vue",
                "https://example.com/app.zip",
                target);
    }
}
