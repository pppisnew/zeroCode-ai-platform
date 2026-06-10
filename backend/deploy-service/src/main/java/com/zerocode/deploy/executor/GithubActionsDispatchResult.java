package com.zerocode.deploy.executor;

public record GithubActionsDispatchResult(
        int statusCode,
        String responseBody) {

    public boolean succeeded() {
        return statusCode == 204;
    }
}
