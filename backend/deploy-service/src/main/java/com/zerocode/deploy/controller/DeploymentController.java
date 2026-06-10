package com.zerocode.deploy.controller;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import com.zerocode.deploy.service.DeploymentService;
import com.zerocode.deploy.vo.ApiResponse;
import com.zerocode.deploy.vo.DeploymentVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/deployments")
public class DeploymentController {
    private final DeploymentService deploymentService;

    public DeploymentController(DeploymentService deploymentService) {
        this.deploymentService = deploymentService;
    }

    @PostMapping
    public ApiResponse<DeploymentVO> createDeployment(
            @Valid @RequestBody CreateDeploymentRequest request) {
        return ApiResponse.ok(deploymentService.createDeployment(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<DeploymentVO> getDeployment(@PathVariable String id) {
        return ApiResponse.ok(deploymentService.getDeployment(id));
    }
}
