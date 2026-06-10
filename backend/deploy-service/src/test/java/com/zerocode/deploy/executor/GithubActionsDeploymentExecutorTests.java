package com.zerocode.deploy.executor;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;

class GithubActionsDeploymentExecutorTests {

    @Test
    void enabledExecutorStaysSkippedOutsideRealMode() {
        RecordingGithubActionsClient client = new RecordingGithubActionsClient(204, "");
        GithubActionsDeploymentExecutor executor = executor("dry-run", "token", client);

        DeploymentExecutionResult result = executor.prepare(request(), List.of("create workflow dispatch"));

        assertThat(result.status()).isEqualTo("skipped");
        assertThat(result.executionLogs())
                .contains(
                        "GitHub Actions executor enabled in dry-run mode",
                        "Set zerocode.deploy.executors.github-actions.execution-mode=real to dispatch workflows",
                        "Skipped: create workflow dispatch");
        assertThat(client.command).isNull();
    }

    @Test
    void realModeSkipsWhenConfigurationIsIncomplete() {
        RecordingGithubActionsClient client = new RecordingGithubActionsClient(204, "");
        GithubActionsDeploymentExecutor executor = executor("real", "", client);

        DeploymentExecutionResult result = executor.prepare(request(), List.of());

        assertThat(result.status()).isEqualTo("skipped");
        assertThat(result.executionLogs())
                .contains("GitHub Actions dispatch skipped because configuration is incomplete: token");
        assertThat(client.command).isNull();
    }

    @Test
    void realModeDispatchesWorkflow() {
        RecordingGithubActionsClient client = new RecordingGithubActionsClient(204, "");
        GithubActionsDeploymentExecutor executor = executor("real", "secret-token", client);

        DeploymentExecutionResult result = executor.prepare(request(), List.of());

        assertThat(result.status()).isEqualTo("succeeded");
        assertThat(result.executionLogs())
                .contains(
                        "Dispatching workflow deploy.yml on pppisnew/zeroCode-ai-platform ref main",
                        "GitHub workflow dispatch accepted");
        assertThat(String.join("\n", result.executionLogs())).doesNotContain("secret-token");
        assertThat(client.command.inputs())
                .containsEntry("app_id", "10")
                .containsEntry("version_no", "2")
                .containsEntry("project_type", "vue")
                .containsEntry("artifact_url", "https://example.com/app.zip");
    }

    @Test
    void realModeReturnsFailedWhenDispatchFails() {
        RecordingGithubActionsClient client = new RecordingGithubActionsClient(404, "missing workflow");
        GithubActionsDeploymentExecutor executor = executor("real", "secret-token", client);

        DeploymentExecutionResult result = executor.prepare(request(), List.of());

        assertThat(result.status()).isEqualTo("failed");
        assertThat(result.executionLogs())
                .contains(
                        "GitHub workflow dispatch failed with status 404",
                        "GitHub response: missing workflow");
    }

    private static GithubActionsDeploymentExecutor executor(
            String executionMode,
            String token,
            GithubActionsClient client) {
        return new GithubActionsDeploymentExecutor(
                true,
                executionMode,
                "https://api.github.test",
                token,
                "pppisnew",
                "zeroCode-ai-platform",
                "deploy.yml",
                "main",
                client);
    }

    private static CreateDeploymentRequest request() {
        return new CreateDeploymentRequest(
                10L,
                2,
                "vue",
                "https://example.com/app.zip",
                "github-actions");
    }

    private static class RecordingGithubActionsClient implements GithubActionsClient {
        private final int statusCode;
        private final String responseBody;
        private GithubActionsDispatchCommand command;

        RecordingGithubActionsClient(int statusCode, String responseBody) {
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        @Override
        public GithubActionsDispatchResult dispatch(
                GithubActionsDispatchCommand command) throws IOException {
            this.command = command;
            return new GithubActionsDispatchResult(statusCode, responseBody);
        }
    }
}
