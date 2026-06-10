package com.zerocode.deploy.service.impl;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import com.zerocode.deploy.executor.DeploymentExecutionResult;
import com.zerocode.deploy.executor.DeploymentExecutorRouter;
import com.zerocode.deploy.model.DeploymentRecord;
import com.zerocode.deploy.repository.DeploymentRepository;
import com.zerocode.deploy.service.DeploymentService;
import com.zerocode.deploy.vo.DeploymentVO;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InMemoryDeploymentService implements DeploymentService {
    private final DeploymentRepository deploymentRepository;
    private final DeploymentExecutorRouter deploymentExecutorRouter;

    public InMemoryDeploymentService(
            DeploymentRepository deploymentRepository,
            DeploymentExecutorRouter deploymentExecutorRouter) {
        this.deploymentRepository = deploymentRepository;
        this.deploymentExecutorRouter = deploymentExecutorRouter;
    }

    @Override
    public DeploymentVO createDeployment(CreateDeploymentRequest request) {
        String id = UUID.randomUUID().toString();
        List<String> plannedCommands = plannedCommands(request);
        DeploymentExecutionResult executionResult = deploymentExecutorRouter.prepare(request, plannedCommands);
        DeploymentRecord deployment = new DeploymentRecord(
                id,
                request.appId(),
                request.versionNo(),
                request.projectType(),
                request.artifactUrl(),
                request.target(),
                executionResult.status(),
                plannedCommands,
                executionResult.executionLogs(),
                executionResult.accessUrl(),
                LocalDateTime.now());
        return toVO(deploymentRepository.save(deployment));
    }

    @Override
    public DeploymentVO getDeployment(String id) {
        return deploymentRepository.findById(id)
                .map(this::toVO)
                .orElseThrow(() -> new IllegalArgumentException("Deployment not found"));
    }

    private List<String> plannedCommands(CreateDeploymentRequest request) {
        if ("github-actions".equals(request.target())) {
            return List.of(
                    "download artifact from " + request.artifactUrl(),
                    "create GitHub Actions workflow dispatch",
                    "wait for workflow result");
        }
        if ("kubernetes".equals(request.target())) {
            return List.of(
                    "download artifact from " + request.artifactUrl(),
                    "docker build -t zerocode/app-" + request.appId() + ":v" + request.versionNo() + " .",
                    "docker push zerocode/app-" + request.appId() + ":v" + request.versionNo(),
                    "kubectl apply -f deployment.yaml");
        }
        return List.of(
                "download artifact from " + request.artifactUrl(),
                "docker build -t zerocode/app-" + request.appId() + ":v" + request.versionNo() + " .",
                "docker run --rm -p 8080:80 zerocode/app-" + request.appId() + ":v" + request.versionNo());
    }

    private DeploymentVO toVO(DeploymentRecord deployment) {
        return new DeploymentVO(
                deployment.id(),
                deployment.appId(),
                deployment.versionNo(),
                deployment.projectType(),
                deployment.artifactUrl(),
                deployment.target(),
                deployment.status(),
                deployment.plannedCommands(),
                deployment.executionLogs(),
                deployment.accessUrl(),
                deployment.createTime());
    }
}
