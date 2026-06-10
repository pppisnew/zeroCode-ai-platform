package com.zerocode.deploy.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class KubernetesDeploymentExecutorTests {

    @Test
    void enabledExecutorStaysSkippedOutsideRealMode() {
        RecordingKubernetesCommandRunner runner = new RecordingKubernetesCommandRunner(0);
        KubernetesDeploymentExecutor executor = executor("dry-run", runner);

        DeploymentExecutionResult result = executor.prepare(request(), List.of("kubectl apply -f deployment.yaml"));

        assertThat(result.status()).isEqualTo("skipped");
        assertThat(result.executionLogs())
                .contains(
                        "Kubernetes executor enabled in dry-run mode",
                        "Set zerocode.deploy.executors.kubernetes.execution-mode=real to execute kubectl",
                        "Skipped: kubectl apply -f deployment.yaml");
        assertThat(runner.commands).isEmpty();
    }

    @Test
    void realModeGeneratesManifestAndAppliesIt() {
        RecordingKubernetesCommandRunner runner = new RecordingKubernetesCommandRunner(0);
        KubernetesDeploymentExecutor executor = executor("real", runner);

        DeploymentExecutionResult result = executor.prepare(request(), List.of());

        assertThat(result.status()).isEqualTo("succeeded");
        assertThat(result.executionLogs())
                .contains(
                        "Kubernetes executor enabled in real mode",
                        "Kubernetes manifest applied");
        assertThat(runner.commands)
                .containsExactly(List.of("kubectl", "apply", "-f", runner.manifestPath.toString(), "-n", "production"));
        assertThat(runner.manifest)
                .contains(
                        "kind: Deployment",
                        "name: zerocode-app-10",
                        "image: registry.test/zerocode/app-10:v2",
                        "kind: Service",
                        "port: 8080");
        assertThat(runner.environment).containsEntry("KUBECONFIG", "/secure/kubeconfig");
    }

    @Test
    void realModeReturnsFailedWhenKubectlFails() {
        RecordingKubernetesCommandRunner runner = new RecordingKubernetesCommandRunner(1);
        KubernetesDeploymentExecutor executor = executor("real", runner);

        DeploymentExecutionResult result = executor.prepare(request(), List.of());

        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.executionLogs())
                .contains("kubectl apply failed with exit code 1");
    }

    private static KubernetesDeploymentExecutor executor(
            String executionMode,
            KubernetesCommandRunner runner) {
        return new KubernetesDeploymentExecutor(
                true,
                executionMode,
                "production",
                "kubectl",
                "/secure/kubeconfig",
                30,
                "registry.test/zerocode",
                8080,
                runner);
    }

    private static CreateDeploymentRequest request() {
        return new CreateDeploymentRequest(
                10L,
                2,
                "vue",
                "https://example.com/app.zip",
                "kubernetes");
    }

    private static class RecordingKubernetesCommandRunner implements KubernetesCommandRunner {
        private final int exitCode;
        private final List<List<String>> commands = new ArrayList<>();
        private Map<String, String> environment = Map.of();
        private Path manifestPath;
        private String manifest;

        RecordingKubernetesCommandRunner(int exitCode) {
            this.exitCode = exitCode;
        }

        @Override
        public KubernetesCommandResult run(
                List<String> command,
                Path workingDirectory,
                Duration timeout,
                Map<String, String> environment) throws java.io.IOException {
            commands.add(command);
            this.environment = environment;
            this.manifestPath = Path.of(command.get(3));
            this.manifest = Files.readString(manifestPath);
            return new KubernetesCommandResult(exitCode, List.of("fake kubectl output"));
        }
    }
}
