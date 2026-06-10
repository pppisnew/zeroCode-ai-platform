package com.zerocode.deploy.executor;

import java.util.Map;

public record GithubActionsDispatchCommand(
        String apiBaseUrl,
        String token,
        String owner,
        String repo,
        String workflowId,
        String ref,
        Map<String, String> inputs) {
}
