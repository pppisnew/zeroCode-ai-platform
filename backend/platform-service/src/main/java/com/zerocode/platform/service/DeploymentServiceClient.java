package com.zerocode.platform.service;

import com.zerocode.platform.dto.DeployServiceDeploymentRequest;
import com.zerocode.platform.vo.DeploymentVO;

public interface DeploymentServiceClient {

    DeploymentVO createDeployment(DeployServiceDeploymentRequest request);
}
