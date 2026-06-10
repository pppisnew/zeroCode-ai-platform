package com.zerocode.deploy.executor;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

public interface DockerCommandRunner {

    DockerCommandResult run(
            List<String> command,
            Path workingDirectory,
            Duration timeout) throws IOException, InterruptedException;
}
