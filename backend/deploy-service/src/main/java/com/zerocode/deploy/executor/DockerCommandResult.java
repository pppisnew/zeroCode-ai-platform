package com.zerocode.deploy.executor;

import java.util.List;

public record DockerCommandResult(
        int exitCode,
        List<String> outputLines) {

    public boolean succeeded() {
        return exitCode == 0;
    }
}
