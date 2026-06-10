package com.zerocode.deploy.executor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

@Component
public class ProcessDockerCommandRunner implements DockerCommandRunner {

    @Override
    public DockerCommandResult run(
            List<String> command,
            Path workingDirectory,
            Duration timeout) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .start();

        boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        if (!finished) {
            process.destroyForcibly();
            return new DockerCommandResult(124, List.of("Command timed out after " + timeout.toSeconds() + "s"));
        }

        String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        List<String> outputLines = output.isBlank()
                ? List.of()
                : output.lines().toList();
        return new DockerCommandResult(process.exitValue(), outputLines);
    }
}
