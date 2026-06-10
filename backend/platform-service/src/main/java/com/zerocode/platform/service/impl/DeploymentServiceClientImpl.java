package com.zerocode.platform.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerocode.platform.dto.DeployServiceDeploymentRequest;
import com.zerocode.platform.service.DeploymentServiceClient;
import com.zerocode.platform.vo.ApiResponse;
import com.zerocode.platform.vo.DeploymentVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class DeploymentServiceClientImpl implements DeploymentServiceClient {

    private static final TypeReference<ApiResponse<DeploymentVO>> RESPONSE_TYPE =
            new TypeReference<>() {
            };

    private final RestClient deployRestClient;
    private final ObjectMapper objectMapper;

    public DeploymentServiceClientImpl(
            @Qualifier("deployRestClient") RestClient deployRestClient,
            ObjectMapper objectMapper) {
        this.deployRestClient = deployRestClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public DeploymentVO createDeployment(DeployServiceDeploymentRequest request) {
        Object response = deployRestClient.post()
                .uri("/deployments")
                .body(request)
                .retrieve()
                .body(Object.class);

        ApiResponse<DeploymentVO> apiResponse = objectMapper.convertValue(response, RESPONSE_TYPE);
        if (apiResponse == null || (apiResponse.code() != 0 && apiResponse.code() != 200)) {
            String message = apiResponse == null ? "Deploy service unavailable" : apiResponse.message();
            throw new IllegalArgumentException(message);
        }
        return apiResponse.data();
    }
}
