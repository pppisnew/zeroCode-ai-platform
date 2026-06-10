package com.zerocode.deploy.executor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class ProcessKubernetesCommandRunner implements KubernetesCommandRunner {

    @Override
    public KubernetesCommandResult run(
            List<String> command,
            Path workingDirectory,
            Duration timeout,
            Map<String, String> environment) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true);
        processBuilder.environment().putAll(environment);
        Process process = processBuilder.start();

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new KubernetesCommandResult(124, List.of("Command timed out after " + timeout.toSeconds() + "s"));
        }

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        List<String> outputLines = output.isBlank()
                ? List.of()
                : output.lines().toList();
        return new KubernetesCommandResult(process.exitValue(), outputLines);
    }
}
