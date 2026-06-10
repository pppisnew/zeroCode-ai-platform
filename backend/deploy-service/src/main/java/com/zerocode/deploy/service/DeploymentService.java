package com.zerocode.deploy.service;

import com.zerocode.deploy.dto.CreateDeploymentRequest;
import com.zerocode.deploy.vo.DeploymentVO;

public interface DeploymentService {
    DeploymentVO createDeployment(CreateDeploymentRequest request);

    DeploymentVO getDeployment(String id);
}
