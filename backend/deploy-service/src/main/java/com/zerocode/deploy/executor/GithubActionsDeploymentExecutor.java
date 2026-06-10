package com.zerocode.deploy.executor;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import com.zerocode.deploy.model.DeploymentStatus;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GithubActionsDeploymentExecutor extends TargetDeploymentExecutor {
    private final String executionMode;
    private final String apiBaseUrl;
    private final String token;
    private final String owner;
    private final String repo;
    private final String workflowId;
    private final String ref;
    private final GithubActionsClient githubActionsClient;

    public GithubActionsDeploymentExecutor(boolean enabled) {
        this(
                enabled,
                "dry-run",
                "https://api.github.com",
                "",
                "",
                "",
                "",
                "main",
                command -> new GithubActionsDispatchResult(204, ""));
    }

    @Autowired
    public GithubActionsDeploymentExecutor(
            @Value("${zerocode.deploy.executors.github-actions.enabled:false}") boolean enabled,
            @Value("${zerocode.deploy.executors.github-actions.execution-mode:dry-run}") String executionMode,
            @Value("${zerocode.deploy.executors.github-actions.api-base-url:https://api.github.com}") String apiBaseUrl,
            @Value("${zerocode.deploy.executors.github-actions.token:}") String token,
            @Value("${zerocode.deploy.executors.github-actions.owner:}") String owner,
            @Value("${zerocode.deploy.executors.github-actions.repo:}") String repo,
            @Value("${zerocode.deploy.executors.github-actions.workflow-id:}") String workflowId,
            @Value("${zerocode.deploy.executors.github-actions.ref:main}") String ref,
            GithubActionsClient githubActionsClient) {
        super("github-actions", enabled);
        this.executionMode = normalized(executionMode, "dry-run");
        this.apiBaseUrl = normalized(apiBaseUrl, "https://api.github.com");
        this.token = normalized(token, "");
        this.owner = normalized(owner, "");
        this.repo = normalized(repo, "");
        this.workflowId = normalized(workflowId, "");
        this.ref = normalized(ref, "main");
        this.githubActionsClient = githubActionsClient;
    }

    @Override
    protected String executorName() {
        return "GitHub Actions";
    }

    @Override
    public DeploymentExecutionResult prepare(
            CreateDeploymentRequest request,
            List<String> plannedCommands) {
        if (!"real".equalsIgnoreCase(executionMode)) {
            return skippedResult(request, plannedCommands);
        }

        List<String> missing = missingConfiguration();
        if (!missing.isEmpty()) {
            List<String> logs = new ArrayList<>();
            logs.add("GitHub Actions executor enabled in real mode");
            logs.add("GitHub Actions dispatch skipped because configuration is incomplete: "
                    + String.join(", ", missing));
            return new DeploymentExecutionResult(DeploymentStatus.SKIPPED.value(), logs, null);
        }

        List<String> logs = new ArrayList<>();
        logs.add("GitHub Actions executor enabled in real mode");
        logs.add("Dispatching workflow " + workflowId + " on " + owner + "/" + repo + " ref " + ref);

        try {
            GithubActionsDispatchResult result = githubActionsClient.dispatch(dispatchCommand(request));
            if (result.succeeded()) {
                logs.add("GitHub workflow dispatch accepted");
                return new DeploymentExecutionResult(DeploymentStatus.SUCCEEDED.value(), logs, null);
            }
            logs.add("GitHub workflow dispatch failed with status " + result.statusCode());
            if (result.responseBody() != null && !result.responseBody().isBlank()) {
                logs.add("GitHub response: " + truncate(result.responseBody()));
            }
            return new DeploymentExecutionResult(DeploymentStatus.FAILED.value(), logs, null);
        } catch (IOException exception) {
            logs.add("GitHub workflow dispatch failed: " + exception.getMessage());
            return new DeploymentExecutionResult(DeploymentStatus.FAILED.value(), logs, null);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            logs.add("GitHub workflow dispatch interrupted");
            return new DeploymentExecutionResult(DeploymentStatus.FAILED.value(), logs, null);
        }
    }

    private DeploymentExecutionResult skippedResult(
            CreateDeploymentRequest request,
            List<String> plannedCommands) {
        List<String> logs = new ArrayList<>();
        logs.add("GitHub Actions executor enabled in " + executionMode + " mode");
        logs.add("Set zerocode.deploy.executors.github-actions.execution-mode=real to dispatch workflows");
        logs.add("Target: " + request.target());
        logs.add("Artifact: " + request.artifactUrl());
        for (String command : plannedCommands) {
            logs.add("Skipped: " + command);
        }
        return new DeploymentExecutionResult(DeploymentStatus.SKIPPED.value(), logs, null);
    }

    private GithubActionsDispatchCommand dispatchCommand(CreateDeploymentRequest request) {
        Map<String, String> inputs = new LinkedHashMap<>();
        inputs.put("app_id", String.valueOf(request.appId()));
        inputs.put("version_no", String.valueOf(request.versionNo()));
        inputs.put("project_type", request.projectType());
        inputs.put("artifact_url", request.artifactUrl());
        return new GithubActionsDispatchCommand(
                apiBaseUrl,
                token,
                owner,
                repo,
                workflowId,
                ref,
                inputs);
    }

    private List<String> missingConfiguration() {
        List<String> missing = new ArrayList<>();
        if (token.isBlank()) {
            missing.add("token");
        }
        if (owner.isBlank()) {
            missing.add("owner");
        }
        if (repo.isBlank()) {
            missing.add("repo");
        }
        if (workflowId.isBlank()) {
            missing.add("workflow-id");
        }
        if (ref.isBlank()) {
            missing.add("ref");
        }
        return missing;
    }

    private static String normalized(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static String truncate(String value) {
        return value.length() <= 1000 ? value : value.substring(0, 1000) + "...";
    }
}
