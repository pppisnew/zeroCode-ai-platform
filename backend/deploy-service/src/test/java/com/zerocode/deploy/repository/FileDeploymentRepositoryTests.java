package com.zerocode.deploy.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerocode.deploy.model.DeploymentRecord;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileDeploymentRepositoryTests {
    @TempDir
    Path tempDir;

    @Test
    void persistsDeploymentRecordsToDisk() {
        Path storePath = tempDir.resolve("deployments.json");
        FileDeploymentRepository repository = new FileDeploymentRepository(
                objectMapper(),
                storePath.toString());
        DeploymentRecord deployment = deployment("deploy-1");

        repository.save(deployment);

        FileDeploymentRepository reloadedRepository = new FileDeploymentRepository(
                objectMapper(),
                storePath.toString());
        assertThat(reloadedRepository.findById("deploy-1"))
                .contains(deployment);
    }

    @Test
    void returnsEmptyWhenDeploymentIsMissing() {
        FileDeploymentRepository repository = new FileDeploymentRepository(
                objectMapper(),
                tempDir.resolve("deployments.json").toString());

        assertThat(repository.findById("missing")).isEmpty();
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    private static DeploymentRecord deployment(String id) {
        return new DeploymentRecord(
                id,
                10L,
                2,
                "vue",
                "https://example.com/app.zip",
                "docker",
                "planned",
                List.of("docker build"),
                List.of("Dry-run deployment: no command executed"),
                null,
                LocalDateTime.of(2026, 6, 10, 10, 30));
    }
}
