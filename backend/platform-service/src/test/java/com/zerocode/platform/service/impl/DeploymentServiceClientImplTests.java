package com.zerocode.platform.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerocode.platform.dto.DeployServiceDeploymentRequest;
import com.zerocode.platform.vo.DeploymentVO;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class DeploymentServiceClientImplTests {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void forwardsCreateDeploymentRequestToDeployService() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://deploy.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        DeploymentServiceClientImpl client = new DeploymentServiceClientImpl(
                restClientBuilder.build(),
                objectMapper);

        server.expect(requestTo("http://deploy.test/deployments"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "appId": 10,
                          "versionNo": 2,
                          "projectType": "vue",
                          "artifactUrl": "http://platform.test/api/apps/10/versions/2/zip",
                          "target": "docker"
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "code": 0,
                          "message": "ok",
                          "data": {
                            "id": "deploy-1",
                            "appId": 10,
                            "versionNo": 2,
                            "projectType": "vue",
                            "artifactUrl": "http://platform.test/api/apps/10/versions/2/zip",
                            "target": "docker",
                            "status": "planned",
                            "plannedCommands": [
                              "docker build -t zerocode-app-10-v2 ."
                            ],
                            "executionLogs": [
                              "Dry-run deployment only"
                            ],
                            "accessUrl": null,
                            "createTime": null
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        DeploymentVO deployment = client.createDeployment(new DeployServiceDeploymentRequest(
                10L,
                2,
                "vue",
                "http://platform.test/api/apps/10/versions/2/zip",
                "docker"));

        assertThat(deployment.id()).isEqualTo("deploy-1");
        assertThat(deployment.status()).isEqualTo("planned");
        assertThat(deployment.plannedCommands())
                .containsExactly("docker build -t zerocode-app-10-v2 .");
        server.verify();
    }

    @Test
    void rejectsDeployServiceErrorEnvelope() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://deploy.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        DeploymentServiceClientImpl client = new DeploymentServiceClientImpl(
                restClientBuilder.build(),
                objectMapper);

        server.expect(requestTo("http://deploy.test/deployments"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess("""
                        {
                          "code": 400,
                          "message": "Invalid deployment target",
                          "data": null
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.createDeployment(new DeployServiceDeploymentRequest(
                10L,
                2,
                "vue",
                "http://platform.test/api/apps/10/versions/2/zip",
                "docker")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid deployment target");
        server.verify();
    }
}
