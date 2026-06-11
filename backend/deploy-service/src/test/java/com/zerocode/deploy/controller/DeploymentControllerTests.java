package com.zerocode.deploy.controller;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zerocode.deploy.config.GlobalExceptionHandler;
import com.zerocode.deploy.dto.CreateDeploymentRequest;
import com.zerocode.deploy.service.DeploymentService;
import com.zerocode.deploy.vo.DeploymentVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class DeploymentControllerTests {
    private final DeploymentService deploymentService = mock(DeploymentService.class);
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new DeploymentController(deploymentService))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void createsDeploymentWithUnifiedEnvelope() throws Exception {
        DeploymentVO deployment = deployment();
        when(deploymentService.createDeployment(any(CreateDeploymentRequest.class))).thenReturn(deployment);

        mockMvc.perform(post("/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appId": 10,
                                  "versionNo": 2,
                                  "projectType": "vue",
                                  "artifactUrl": "https://example.com/app.zip",
                                  "target": "docker"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("ok"))
                .andExpect(jsonPath("$.data.id").value("deploy-1"))
                .andExpect(jsonPath("$.data.status").value("planned"))
                .andExpect(jsonPath("$.data.plannedCommands[0]").value("download artifact"))
                .andExpect(jsonPath("$.data.executionLogs[0]").value("Dry-run deployment"));
        verify(deploymentService).createDeployment(any(CreateDeploymentRequest.class));
    }

    @Test
    void getsDeploymentWithUnifiedEnvelope() throws Exception {
        when(deploymentService.getDeployment("deploy-1")).thenReturn(deployment());

        mockMvc.perform(get("/deployments/{id}", "deploy-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("deploy-1"))
                .andExpect(jsonPath("$.data.createTime", notNullValue()));
    }

    @Test
    void validationErrorsUseUnifiedEnvelope() throws Exception {
        mockMvc.perform(post("/deployments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "appId": 0,
                                  "versionNo": 2,
                                  "projectType": "svelte",
                                  "artifactUrl": "",
                                  "target": "docker"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "code": 400,
                          "data": null,
                          "message": "Invalid request"
                        }
                        """));
    }

    @Test
    void missingDeploymentUsesUnifiedEnvelope() throws Exception {
        when(deploymentService.getDeployment("missing"))
                .thenThrow(new IllegalArgumentException("Deployment not found"));

        mockMvc.perform(get("/deployments/{id}", "missing"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "code": 400,
                          "data": null,
                          "message": "Invalid request"
                        }
                        """));
    }

    private static DeploymentVO deployment() {
        return new DeploymentVO(
                "deploy-1",
                10L,
                2,
                "vue",
                "https://example.com/app.zip",
                "docker",
                "planned",
                List.of("download artifact"),
                List.of("Dry-run deployment"),
                null,
                LocalDateTime.of(2026, 6, 8, 16, 30));
    }
}
