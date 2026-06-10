package com.zerocode.deploy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import com.zerocode.deploy.executor.DeploymentExecutorRouter;
import com.zerocode.deploy.executor.DockerDeploymentExecutor;
import com.zerocode.deploy.executor.DryRunDeploymentExecutor;
import com.zerocode.deploy.model.DeploymentRecord;
import com.zerocode.deploy.repository.DeploymentRepository;
import com.zerocode.deploy.service.impl.InMemoryDeploymentService;
import com.zerocode.deploy.vo.DeploymentVO;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InMemoryDeploymentServiceTests {
    private final InMemoryDeploymentService deploymentService = new InMemoryDeploymentService(
            new MapDeploymentRepository(),
            new DeploymentExecutorRouter(
                    List.of(new DryRunDeploymentExecutor()),
                    new DryRunDeploymentExecutor()));

    @Test
    void createsPlannedDockerDeployment() {
        DeploymentVO deployment = deploymentService.createDeployment(request("docker"));

        assertThat(deployment.id()).isNotBlank();
        assertThat(deployment.status()).isEqualTo("planned");
        assertThat(deployment.accessUrl()).isNull();
        assertThat(deployment.plannedCommands())
                .contains(
                        "download artifact from https://example.com/app.zip",
                        "docker build -t zerocode/app-10:v2 .",
                        "docker run --rm -p 8080:80 zerocode/app-10:v2");
        assertThat(deployment.executionLogs())
                .contains(
                        "Dry-run deployment: no command executed",
                        "Target: docker",
                        "Artifact: https://example.com/app.zip");
        assertThat(deploymentService.getDeployment(deployment.id())).isEqualTo(deployment);
    }

    @Test
    void createsTargetSpecificDeploymentPlans() {
        assertThat(deploymentService.createDeployment(request("github-actions")).plannedCommands())
                .contains("create GitHub Actions workflow dispatch");
        assertThat(deploymentService.createDeployment(request("kubernetes")).plannedCommands())
                .contains("kubectl apply -f deployment.yaml");
    }

    @Test
    void recordsSkippedStatusWhenTargetExecutorIsExplicitlyEnabled() {
        DryRunDeploymentExecutor dryRunExecutor = new DryRunDeploymentExecutor();
        InMemoryDeploymentService service = new InMemoryDeploymentService(
                new MapDeploymentRepository(),
                new DeploymentExecutorRouter(
                        List.of(new DockerDeploymentExecutor(true), dryRunExecutor),
                        dryRunExecutor));

        DeploymentVO deployment = service.createDeployment(request("docker"));

        assertThat(deployment.status()).isEqualTo("skipped");
        assertThat(deployment.executionLogs())
                .contains(
                        "Docker executor enabled in dry-run mode",
                        "Set zerocode.deploy.executors.docker.execution-mode=real to execute Docker commands");
        assertThat(service.getDeployment(deployment.id()).status()).isEqualTo("skipped");
    }

    @Test
    void rejectsUnknownDeploymentId() {
        assertThatThrownBy(() -> deploymentService.getDeployment("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Deployment not found");
    }

    private static CreateDeploymentRequest request(String target) {
        return new CreateDeploymentRequest(
                10L,
                2,
                "vue",
                "https://example.com/app.zip",
                target);
    }

    private static class MapDeploymentRepository implements DeploymentRepository {
        private final Map<String, DeploymentRecord> deployments = new HashMap<>();

        @Override
        public DeploymentRecord save(DeploymentRecord deployment) {
            deployments.put(deployment.id(), deployment);
            return deployment;
        }

        @Override
        public Optional<DeploymentRecord> findById(String id) {
            return Optional.ofNullable(deployments.get(id));
        }
    }
}
