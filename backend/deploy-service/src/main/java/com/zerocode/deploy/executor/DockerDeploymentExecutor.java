package com.zerocode.deploy.executor;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import com.zerocode.deploy.model.DeploymentStatus;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DockerDeploymentExecutor extends TargetDeploymentExecutor {
    private final String executionMode;
    private final Path workspaceRoot;
    private final Duration commandTimeout;
    private final String imageRepositoryPrefix;
    private final boolean pushEnabled;
    private final DockerCommandRunner commandRunner;

    public DockerDeploymentExecutor(boolean enabled) {
        this(
                enabled,
                "dry-run",
                "/tmp/zerocode-docker-deployments",
                300,
                "zerocode",
                false,
                new ProcessDockerCommandRunner());
    }

    @Autowired
    public DockerDeploymentExecutor(
            @Value("${zerocode.deploy.executors.docker.enabled:false}") boolean enabled,
            @Value("${zerocode.deploy.executors.docker.execution-mode:dry-run}") String executionMode,
            @Value("${zerocode.deploy.executors.docker.workspace-root:/tmp/zerocode-docker-deployments}")
                    String workspaceRoot,
            @Value("${zerocode.deploy.executors.docker.command-timeout-seconds:300}") long commandTimeoutSeconds,
            @Value("${zerocode.deploy.executors.docker.image-repository-prefix:zerocode}") String imageRepositoryPrefix,
            @Value("${zerocode.deploy.executors.docker.push-enabled:false}") boolean pushEnabled,
            DockerCommandRunner commandRunner) {
        super("docker", enabled);
        this.executionMode = executionMode == null ? "dry-run" : executionMode.trim();
        this.workspaceRoot = Path.of(workspaceRoot);
        this.commandTimeout = Duration.ofSeconds(commandTimeoutSeconds);
        this.imageRepositoryPrefix = imageRepositoryPrefix == null || imageRepositoryPrefix.isBlank()
                ? "zerocode"
                : imageRepositoryPrefix.trim();
        this.pushEnabled = pushEnabled;
        this.commandRunner = commandRunner;
    }

    @Override
    protected String executorName() {
        return "Docker";
    }

    @Override
    public DeploymentExecutionResult prepare(
            CreateDeploymentRequest request,
            List<String> plannedCommands) {
        if (!"real".equalsIgnoreCase(executionMode)) {
            return skippedResult(request, plannedCommands);
        }

        List<String> logs = new ArrayList<>();
        Path workspace = null;
        try {
            Files.createDirectories(workspaceRoot);
            workspace = Files.createTempDirectory(workspaceRoot, "deploy-" + request.appId() + "-v" + request.versionNo() + "-");
            Path artifactZip = workspace.resolve("artifact.zip");
            logs.add("Docker executor enabled in real mode");
            logs.add("Workspace: " + workspace);
            logs.add("Downloading artifact: " + request.artifactUrl());
            downloadArtifact(request.artifactUrl(), artifactZip);
            logs.add("Extracting artifact");
            extractZipSafely(artifactZip, workspace);

            String imageTag = imageTag(request);
            DockerCommandResult buildResult = runDockerCommand(
                    List.of("docker", "build", "-t", imageTag, "."),
                    workspace,
                    logs);
            if (!buildResult.succeeded()) {
                logs.add("Docker build failed with exit code " + buildResult.exitCode());
                return new DeploymentExecutionResult(DeploymentStatus.FAILED.value(), logs, null);
            }

            if (pushEnabled) {
                DockerCommandResult pushResult = runDockerCommand(
                        List.of("docker", "push", imageTag),
                        workspace,
                        logs);
                if (!pushResult.succeeded()) {
                    logs.add("Docker push failed with exit code " + pushResult.exitCode());
                    return new DeploymentExecutionResult(DeploymentStatus.FAILED.value(), logs, null);
                }
            } else {
                logs.add("Docker push skipped because push-enabled=false");
            }

            logs.add("Docker deployment artifact built: " + imageTag);
            return new DeploymentExecutionResult(DeploymentStatus.SUCCEEDED.value(), logs, null);
        } catch (IOException | IllegalArgumentException exception) {
            logs.add("Docker deployment failed: " + exception.getMessage());
            return new DeploymentExecutionResult(DeploymentStatus.FAILED.value(), logs, null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logs.add("Docker deployment interrupted");
            return new DeploymentExecutionResult(DeploymentStatus.FAILED.value(), logs, null);
        } finally {
            cleanupWorkspace(workspace, logs);
        }
    }

    private DeploymentExecutionResult skippedResult(
            CreateDeploymentRequest request,
            List<String> plannedCommands) {
        List<String> logs = new ArrayList<>();
        logs.add("Docker executor enabled in " + executionMode + " mode");
        logs.add("Set zerocode.deploy.executors.docker.execution-mode=real to execute Docker commands");
        logs.add("Target: " + request.target());
        logs.add("Artifact: " + request.artifactUrl());
        for (String command : plannedCommands) {
            logs.add("Skipped: " + command);
        }
        return new DeploymentExecutionResult(DeploymentStatus.SKIPPED.value(), logs, null);
    }

    private DockerCommandResult runDockerCommand(
            List<String> command,
            Path workspace,
            List<String> logs) throws IOException, InterruptedException {
        logs.add("$ " + String.join(" ", command));
        DockerCommandResult result = commandRunner.run(command, workspace, commandTimeout);
        logs.addAll(result.outputLines());
        return result;
    }

    private static final long MAX_ARTIFACT_DOWNLOAD_BYTES = 500 * 1024 * 1024;

    private void downloadArtifact(String artifactUrl, Path artifactZip) throws IOException {
        URI artifactUri = URI.create(artifactUrl);
        String scheme = artifactUri.getScheme();
        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme) && !"file".equalsIgnoreCase(scheme)) {
            throw new IllegalArgumentException("Artifact URL must use http, https or file scheme");
        }
        URL url = artifactUri.toURL();
        try (InputStream inputStream = url.openStream()) {
            byte[] buffer = new byte[8192];
            long totalRead = 0;
            int bytesRead;
            try (java.io.OutputStream outputStream = Files.newOutputStream(artifactZip)) {
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    totalRead += bytesRead;
                    if (totalRead > MAX_ARTIFACT_DOWNLOAD_BYTES) {
                        throw new IllegalArgumentException("Artifact download exceeds size limit");
                    }
                    outputStream.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    private void extractZipSafely(Path artifactZip, Path workspace) throws IOException {
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(artifactZip))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path target = workspace.resolve(entry.getName()).normalize();
                if (!target.startsWith(workspace)) {
                    throw new IllegalArgumentException("Invalid zip entry path");
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(target);
                } else {
                    Path parent = target.getParent();
                    if (parent != null) {
                        Files.createDirectories(parent);
                    }
                    Files.copy(zipInputStream, target, StandardCopyOption.REPLACE_EXISTING);
                }
                zipInputStream.closeEntry();
            }
        }
        Files.deleteIfExists(artifactZip);
    }

    private String imageTag(CreateDeploymentRequest request) {
        String imageName = "app-" + request.appId() + ":v" + request.versionNo();
        return (imageRepositoryPrefix + "/" + imageName).toLowerCase();
    }

    private void cleanupWorkspace(Path workspace, List<String> logs) {
        if (workspace == null || !workspace.startsWith(workspaceRoot)) {
            return;
        }
        try {
            try (var paths = Files.walk(workspace)) {
                for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(path);
                }
            }
        } catch (IOException exception) {
            logs.add("Workspace cleanup failed: " + exception.getMessage());
        }
    }
}
