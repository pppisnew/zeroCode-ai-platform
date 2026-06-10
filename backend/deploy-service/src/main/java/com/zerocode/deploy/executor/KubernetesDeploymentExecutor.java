package com.zerocode.deploy.executor;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import com.zerocode.deploy.model.DeploymentStatus;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KubernetesDeploymentExecutor extends TargetDeploymentExecutor {
    private final String executionMode;
    private final String namespace;
    private final String kubectlBinary;
    private final String kubeconfig;
    private final Duration commandTimeout;
    private final String imageRepositoryPrefix;
    private final int servicePort;
    private final KubernetesCommandRunner commandRunner;

    public KubernetesDeploymentExecutor(boolean enabled) {
        this(
                enabled,
                "dry-run",
                "default",
                "kubectl",
                "",
                300,
                "zerocode",
                80,
                new ProcessKubernetesCommandRunner());
    }

    @Autowired
    public KubernetesDeploymentExecutor(
            @Value("${zerocode.deploy.executors.kubernetes.enabled:false}") boolean enabled,
            @Value("${zerocode.deploy.executors.kubernetes.execution-mode:dry-run}") String executionMode,
            @Value("${zerocode.deploy.executors.kubernetes.namespace:default}") String namespace,
            @Value("${zerocode.deploy.executors.kubernetes.kubectl-binary:kubectl}") String kubectlBinary,
            @Value("${zerocode.deploy.executors.kubernetes.kubeconfig:}") String kubeconfig,
            @Value("${zerocode.deploy.executors.kubernetes.command-timeout-seconds:300}") long commandTimeoutSeconds,
            @Value("${zerocode.deploy.executors.kubernetes.image-repository-prefix:zerocode}") String imageRepositoryPrefix,
            @Value("${zerocode.deploy.executors.kubernetes.service-port:80}") int servicePort,
            KubernetesCommandRunner commandRunner) {
        super("kubernetes", enabled);
        this.executionMode = normalized(executionMode, "dry-run");
        this.namespace = normalized(namespace, "default");
        this.kubectlBinary = normalized(kubectlBinary, "kubectl");
        this.kubeconfig = normalized(kubeconfig, "");
        this.commandTimeout = Duration.ofSeconds(commandTimeoutSeconds);
        this.imageRepositoryPrefix = normalized(imageRepositoryPrefix, "zerocode");
        this.servicePort = servicePort;
        this.commandRunner = commandRunner;
    }

    @Override
    protected String executorName() {
        return "Kubernetes";
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
            workspace = Files.createTempDirectory("zerocode-kubernetes-deploy-");
            Path manifest = workspace.resolve("deployment.yaml");
            Files.writeString(manifest, manifest(request), StandardCharsets.UTF_8);
            logs.add("Kubernetes executor enabled in real mode");
            logs.add("Generated manifest: " + manifest.getFileName());

            List<String> command = List.of(kubectlBinary, "apply", "-f", manifest.toString(), "-n", namespace);
            logs.add("$ " + String.join(" ", command));
            KubernetesCommandResult result = commandRunner.run(command, workspace, commandTimeout, environment());
            logs.addAll(result.outputLines());
            if (!result.succeeded()) {
                logs.add("kubectl apply failed with exit code " + result.exitCode());
                return new DeploymentExecutionResult(DeploymentStatus.FAILED.value(), logs, null);
            }
            logs.add("Kubernetes manifest applied");
            return new DeploymentExecutionResult(DeploymentStatus.SUCCEEDED.value(), logs, null);
        } catch (IOException | IllegalArgumentException exception) {
            logs.add("Kubernetes deployment failed: " + exception.getMessage());
            return new DeploymentExecutionResult(DeploymentStatus.FAILED.value(), logs, null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logs.add("Kubernetes deployment interrupted");
            return new DeploymentExecutionResult(DeploymentStatus.FAILED.value(), logs, null);
        } finally {
            cleanupWorkspace(workspace, logs);
        }
    }

    private DeploymentExecutionResult skippedResult(
            CreateDeploymentRequest request,
            List<String> plannedCommands) {
        List<String> logs = new ArrayList<>();
        logs.add("Kubernetes executor enabled in " + executionMode + " mode");
        logs.add("Set zerocode.deploy.executors.kubernetes.execution-mode=real to execute kubectl");
        logs.add("Target: " + request.target());
        logs.add("Artifact: " + request.artifactUrl());
        for (String command : plannedCommands) {
            logs.add("Skipped: " + command);
        }
        return new DeploymentExecutionResult(DeploymentStatus.SKIPPED.value(), logs, null);
    }

    private String manifest(CreateDeploymentRequest request) {
        String name = resourceName(request);
        String image = imageTag(request);
        return """
                apiVersion: apps/v1
                kind: Deployment
                metadata:
                  name: %s
                  labels:
                    app: %s
                spec:
                  replicas: 1
                  selector:
                    matchLabels:
                      app: %s
                  template:
                    metadata:
                      labels:
                        app: %s
                    spec:
                      containers:
                        - name: app
                          image: %s
                          ports:
                            - containerPort: 80
                ---
                apiVersion: v1
                kind: Service
                metadata:
                  name: %s
                spec:
                  type: ClusterIP
                  selector:
                    app: %s
                  ports:
                    - port: %d
                      targetPort: 80
                """.formatted(name, name, name, name, image, name, name, servicePort);
    }

    private Map<String, String> environment() {
        Map<String, String> environment = new HashMap<>();
        if (!kubeconfig.isBlank()) {
            environment.put("KUBECONFIG", kubeconfig);
        }
        return environment;
    }

    private String resourceName(CreateDeploymentRequest request) {
        return "zerocode-app-" + request.appId();
    }

    private String imageTag(CreateDeploymentRequest request) {
        return (imageRepositoryPrefix + "/app-" + request.appId() + ":v" + request.versionNo()).toLowerCase();
    }

    private void cleanupWorkspace(Path workspace, List<String> logs) {
        if (workspace == null) {
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

    private static String normalized(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
