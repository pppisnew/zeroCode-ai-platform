package com.zerocode.deploy.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DockerDeploymentExecutorTests {

    @TempDir
    private Path tempDir;

    @Test
    void enabledExecutorStaysSkippedOutsideRealMode() {
        RecordingDockerCommandRunner runner = new RecordingDockerCommandRunner(0);
        DockerDeploymentExecutor executor = new DockerDeploymentExecutor(
                true,
                "dry-run",
                tempDir.resolve("workspace").toString(),
                30,
                "registry.test/zerocode",
                false,
                runner);

        DeploymentExecutionResult result = executor.prepare(request("file:///tmp/app.zip"), List.of("docker build ."));

        assertThat(result.status()).isEqualTo("skipped");
        assertThat(result.executionLogs())
                .contains(
                        "Docker executor enabled in dry-run mode",
                        "Set zerocode.deploy.executors.docker.execution-mode=real to execute Docker commands",
                        "Skipped: docker build .");
        assertThat(runner.commands).isEmpty();
    }

    @Test
    void realModeDownloadsArtifactAndBuildsDockerImage() throws IOException {
        Path artifact = createArtifactZip("Dockerfile", "FROM nginx:1.27-alpine\n");
        RecordingDockerCommandRunner runner = new RecordingDockerCommandRunner(0);
        DockerDeploymentExecutor executor = new DockerDeploymentExecutor(
                true,
                "real",
                tempDir.resolve("workspace").toString(),
                30,
                "registry.test/zerocode",
                false,
                runner);

        DeploymentExecutionResult result = executor.prepare(request(artifact.toUri().toString()), List.of());

        assertThat(result.status()).isEqualTo("succeeded");
        assertThat(result.executionLogs())
                .contains(
                        "Docker executor enabled in real mode",
                        "$ docker build -t registry.test/zerocode/app-10:v2 .",
                        "Docker push skipped because push-enabled=false",
                        "Docker deployment artifact built: registry.test/zerocode/app-10:v2");
        assertThat(runner.commands)
                .containsExactly(List.of("docker", "build", "-t", "registry.test/zerocode/app-10:v2", "."));
        assertThat(runner.dockerfileObserved).isTrue();
    }

    @Test
    void realModeReturnsFailedWhenDockerBuildFails() throws IOException {
        Path artifact = createArtifactZip("Dockerfile", "FROM nginx:1.27-alpine\n");
        RecordingDockerCommandRunner runner = new RecordingDockerCommandRunner(1);
        DockerDeploymentExecutor executor = new DockerDeploymentExecutor(
                true,
                "real",
                tempDir.resolve("workspace").toString(),
                30,
                "registry.test/zerocode",
                false,
                runner);

        DeploymentExecutionResult result = executor.prepare(request(artifact.toUri().toString()), List.of());

        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.executionLogs())
                .contains("Docker build failed with exit code 1");
    }

    @Test
    void realModeRejectsZipSlipArtifact() throws IOException {
        Path artifact = createArtifactZip("../Dockerfile", "FROM nginx:1.27-alpine\n");
        RecordingDockerCommandRunner runner = new RecordingDockerCommandRunner(0);
        DockerDeploymentExecutor executor = new DockerDeploymentExecutor(
                true,
                "real",
                tempDir.resolve("workspace").toString(),
                30,
                "registry.test/zerocode",
                false,
                runner);

        DeploymentExecutionResult result = executor.prepare(request(artifact.toUri().toString()), List.of());

        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.executionLogs()).contains("Docker deployment failed: Invalid zip entry path");
        assertThat(runner.commands).isEmpty();
    }

    private Path createArtifactZip(String path, String content) throws IOException {
        Path artifact = tempDir.resolve("artifact-" + Math.abs(path.hashCode()) + ".zip");
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(Files.newOutputStream(artifact))) {
            zipOutputStream.putNextEntry(new ZipEntry(path));
            zipOutputStream.write(content.getBytes());
            zipOutputStream.closeEntry();
        }
        return artifact;
    }

    private static CreateDeploymentRequest request(String artifactUrl) {
        return new CreateDeploymentRequest(
                10L,
                2,
                "vue",
                artifactUrl,
                "docker");
    }

    private static class RecordingDockerCommandRunner implements DockerCommandRunner {
        private final int exitCode;
        private final List<List<String>> commands = new ArrayList<>();
        private boolean dockerfileObserved;

        RecordingDockerCommandRunner(int exitCode) {
            this.exitCode = exitCode;
        }

        @Override
        public DockerCommandResult run(
                List<String> command,
                Path workingDirectory,
                Duration timeout) {
            commands.add(command);
            dockerfileObserved = Files.exists(workingDirectory.resolve("Dockerfile"));
            return new DockerCommandResult(exitCode, List.of("fake docker output"));
        }
    }
}
