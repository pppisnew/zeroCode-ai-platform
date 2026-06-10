package com.zerocode.deploy.executor;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public interface KubernetesCommandRunner {

    KubernetesCommandResult run(
            List<String> command,
            Path workingDirectory,
            Duration timeout,
            Map<String, String> environment) throws IOException, InterruptedException;
}
