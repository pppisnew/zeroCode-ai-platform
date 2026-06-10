package com.zerocode.deploy.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerocode.deploy.model.DeploymentRecord;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

@Repository
public class FileDeploymentRepository implements DeploymentRepository {
    private static final TypeReference<LinkedHashMap<String, DeploymentRecord>> STORE_TYPE =
            new TypeReference<>() {
            };

    private final ObjectMapper objectMapper;
    private final Path storePath;

    public FileDeploymentRepository(
            ObjectMapper objectMapper,
            @Value("${zerocode.deploy.store-path:/tmp/zerocode-deployments.json}") String storePath) {
        this.objectMapper = objectMapper;
        this.storePath = Path.of(storePath);
    }

    @Override
    public synchronized DeploymentRecord save(DeploymentRecord deployment) {
        Map<String, DeploymentRecord> deployments = readStore();
        deployments.put(deployment.id(), deployment);
        writeStore(deployments);
        return deployment;
    }

    @Override
    public synchronized Optional<DeploymentRecord> findById(String id) {
        return Optional.ofNullable(readStore().get(id));
    }

    private Map<String, DeploymentRecord> readStore() {
        if (!Files.exists(storePath)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(storePath.toFile(), STORE_TYPE);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read deployment store");
        }
    }

    private void writeStore(Map<String, DeploymentRecord> deployments) {
        try {
            Path parent = storePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storePath.toFile(), deployments);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to write deployment store");
        }
    }
}
