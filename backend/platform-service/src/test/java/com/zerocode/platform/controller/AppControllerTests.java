package com.zerocode.platform.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zerocode.platform.config.DeployServiceProperties;
import com.zerocode.platform.config.GlobalExceptionHandler;
import com.zerocode.platform.dto.CreateAppVersionRequest;
import com.zerocode.platform.dto.DeployServiceDeploymentRequest;
import com.zerocode.platform.dto.GeneratedFileRequest;
import com.zerocode.platform.dto.GeneratedProjectRequest;
import com.zerocode.platform.service.AppService;
import com.zerocode.platform.service.AppVersionService;
import com.zerocode.platform.service.DeploymentServiceClient;
import com.zerocode.platform.vo.AppVersionVO;
import com.zerocode.platform.vo.AppVO;
import com.zerocode.platform.vo.DeploymentVO;
import com.zerocode.platform.vo.GeneratedFileVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AppControllerTests {

    private final AppService appService = mock(AppService.class);
    private final AppVersionService appVersionService = mock(AppVersionService.class);
    private final DeploymentServiceClient deploymentServiceClient = mock(DeploymentServiceClient.class);
    private final AppController appController = new AppController(
            appService,
            appVersionService,
            deploymentServiceClient,
            new DeployServiceProperties("http://deploy.test", "http://platform.test/api"));

    @Test
    void exportsVersionFilesAsZipEntries() throws Exception {
        AppVersionVO version = versionWithFiles(List.of(
                new GeneratedFileVO("index.html", "html", "<main>Hello</main>"),
                new GeneratedFileVO("src/App.vue", "vue", "<template><main /></template>")));
        when(appService.getApp(10L)).thenReturn(app());
        when(appVersionService.getVersion(10L, 2)).thenReturn(version);

        ResponseEntity<byte[]> response = appController.exportVersionZip(10L, 2);

        assertThat(response.getHeaders().getContentDisposition().getFilename())
                .isEqualTo("zerocode-app-10-v2.zip");
        assertThat(readZipEntries(response.getBody()))
                .containsEntry("index.html", "<main>Hello</main>")
                .containsEntry("src/App.vue", "<template><main /></template>")
                .containsKey("Dockerfile")
                .containsKey("nginx.conf")
                .containsKey("DEPLOYMENT.md");
        verify(appService).getApp(10L);
        verify(appVersionService).getVersion(10L, 2);
    }

    @Test
    void exportZipDoesNotOverwriteProjectDeploymentFiles() throws Exception {
        AppVersionVO version = versionWithFiles(List.of(
                new GeneratedFileVO("index.html", "html", "<main>Hello</main>"),
                new GeneratedFileVO("Dockerfile", "dockerfile", "FROM custom")));
        when(appService.getApp(10L)).thenReturn(app());
        when(appVersionService.getVersion(10L, 2)).thenReturn(version);

        ResponseEntity<byte[]> response = appController.exportVersionZip(10L, 2);

        assertThat(readZipEntries(response.getBody()))
                .containsEntry("Dockerfile", "FROM custom")
                .containsKey("nginx.conf")
                .containsKey("DEPLOYMENT.md");
    }

    @Test
    void rejectsUnsafeZipPaths() {
        AppVersionVO version = versionWithFiles(List.of(
                new GeneratedFileVO("src/..", "txt", "bad")));
        when(appService.getApp(10L)).thenReturn(app());
        when(appVersionService.getVersion(10L, 1)).thenReturn(version);

        assertThatThrownBy(() -> appController.exportVersionZip(10L, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid file path");
    }

    @Test
    void rejectsDuplicateZipPaths() {
        AppVersionVO version = versionWithFiles(List.of(
                new GeneratedFileVO("src/App.vue", "vue", "one"),
                new GeneratedFileVO("src\\App.vue", "vue", "two")));
        when(appService.getApp(10L)).thenReturn(app());
        when(appVersionService.getVersion(10L, 1)).thenReturn(version);

        assertThatThrownBy(() -> appController.exportVersionZip(10L, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate file path");
    }

    @Test
    void rejectsDangerousZipContent() {
        AppVersionVO version = versionWithFiles(List.of(
                new GeneratedFileVO("src/App.tsx", "tsx", "fetch('/api/data')")));
        when(appService.getApp(10L)).thenReturn(app());
        when(appVersionService.getVersion(10L, 1)).thenReturn(version);

        assertThatThrownBy(() -> appController.exportVersionZip(10L, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project file must not perform network requests");
    }

    @Test
    void rejectsUnsafePathsWhenCreatingVersion() {
        CreateAppVersionRequest request = new CreateAppVersionRequest(
                "manual save",
                new GeneratedProjectRequest(
                        "zerocode-html-app",
                        "html",
                        List.of(new GeneratedFileRequest("src//App.vue", "vue", "bad"))));
        when(appService.getApp(10L)).thenReturn(app());

        assertThatThrownBy(() -> appController.createVersion(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid file path");
        verify(appService).getApp(10L);
        verify(appVersionService, never()).createVersion(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void rejectsDuplicatePathsWhenCreatingVersion() {
        CreateAppVersionRequest request = new CreateAppVersionRequest(
                "manual save",
                new GeneratedProjectRequest(
                        "zerocode-html-app",
                        "vue",
                        List.of(
                                new GeneratedFileRequest("src/App.vue", "vue", "one"),
                                new GeneratedFileRequest("src\\App.vue", "vue", "two"))));
        when(appService.getApp(10L)).thenReturn(app());

        assertThatThrownBy(() -> appController.createVersion(10L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate file path");
        verify(appService).getApp(10L);
        verify(appVersionService, never()).createVersion(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void unsafePathHttpResponseUsesUnifiedEnvelope() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(appController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        when(appService.getApp(10L)).thenReturn(app());

        mockMvc.perform(post("/apps/{id}/versions", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prompt": "manual save",
                                  "project": {
                                    "projectName": "zerocode-html-app",
                                    "projectType": "html",
                                    "files": [
                                      {
                                        "filePath": "src/..",
                                        "fileType": "txt",
                                        "content": "bad"
                                      }
                                    ]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "code": 400,
                          "data": null,
                          "message": "Invalid file path"
                        }
                        """));
        verify(appVersionService, never()).createVersion(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dangerousContentHttpResponseUsesUnifiedEnvelopeBeforeCreatingVersion() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(appController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        when(appService.getApp(10L)).thenReturn(app());

        mockMvc.perform(post("/apps/{id}/versions", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prompt": "manual save",
                                  "project": {
                                    "projectName": "zerocode-react-app",
                                    "projectType": "react",
                                    "files": [
                                      {
                                        "filePath": "src/App.tsx",
                                        "fileType": "tsx",
                                        "content": "fetch('/api/data')"
                                      }
                                    ]
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "code": 400,
                          "data": null,
                          "message": "Project file must not perform network requests"
                        }
                        """));
        verify(appVersionService, never()).createVersion(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void oversizedFileContentResponseUsesUnifiedEnvelopeBeforeCreatingVersion() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(appController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        when(appService.getApp(10L)).thenReturn(app());

        mockMvc.perform(post("/apps/{id}/versions", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "prompt": "manual save",
                                  "project": {
                                    "projectName": "zerocode-html-app",
                                    "projectType": "html",
                                    "files": [
                                      {
                                        "filePath": "index.html",
                                        "fileType": "html",
                                        "content": "%s"
                                      }
                                    ]
                                  }
                                }
                                """.formatted("x".repeat(200_001))))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "code": 400,
                          "data": null,
                          "message": "Invalid request"
                        }
                        """));
        verify(appVersionService, never()).createVersion(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void malformedJsonResponseUsesUnifiedEnvelope() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(appController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/apps/{id}/versions", 10L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not-json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "code": 400,
                          "data": null,
                          "message": "Invalid request body"
                        }
                        """));
        verify(appVersionService, never()).createVersion(
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dangerousZipContentHttpResponseUsesUnifiedEnvelope() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(appController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        AppVersionVO version = versionWithFiles(List.of(
                new GeneratedFileVO("src/App.tsx", "tsx", "fetch('/api/data')")));
        when(appService.getApp(10L)).thenReturn(app());
        when(appVersionService.getVersion(10L, 1)).thenReturn(version);

        mockMvc.perform(get("/apps/{id}/versions/{versionNo}/zip", 10L, 1))
                .andExpect(status().isBadRequest())
                .andExpect(content().json("""
                        {
                          "code": 400,
                          "data": null,
                          "message": "Project file must not perform network requests"
                        }
                        """));
    }

    @Test
    void createsDeploymentForSavedVersion() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(appController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        AppVersionVO version = versionWithFiles(List.of(
                new GeneratedFileVO("src/App.vue", "vue", "<template><main /></template>")));
        DeploymentVO deployment = deployment("deploy-1", "docker", "planned");
        when(appService.getApp(10L)).thenReturn(app());
        when(appVersionService.getVersion(10L, 2)).thenReturn(version);
        when(deploymentServiceClient.createDeployment(org.mockito.ArgumentMatchers.any()))
                .thenReturn(deployment);

        mockMvc.perform(post("/apps/{id}/versions/{versionNo}/deployments", 10L, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "target": "docker"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(content().json("""
                        {
                          "code": 0,
                          "message": "ok",
                          "data": {
                            "id": "deploy-1",
                            "appId": 10,
                            "versionNo": 2,
                            "projectType": "html",
                            "artifactUrl": "http://platform.test/api/apps/10/versions/2/zip",
                            "target": "docker",
                            "status": "planned"
                          }
                        }
                        """));

        ArgumentCaptor<DeployServiceDeploymentRequest> captor =
                ArgumentCaptor.forClass(DeployServiceDeploymentRequest.class);
        verify(deploymentServiceClient).createDeployment(captor.capture());
        assertThat(captor.getValue()).isEqualTo(new DeployServiceDeploymentRequest(
                10L,
                2,
                "html",
                "http://platform.test/api/apps/10/versions/2/zip",
                "docker"));
    }

    @Test
    void invalidDeploymentTargetUsesUnifiedEnvelopeBeforeCallingDeployService() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(appController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mockMvc.perform(post("/apps/{id}/versions/{versionNo}/deployments", 10L, 2)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "target": "shell"
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
        verify(deploymentServiceClient, never()).createDeployment(
                org.mockito.ArgumentMatchers.any());
    }

    private AppVO app() {
        return new AppVO(
                10L,
                1L,
                "zerocode-html-app",
                "demo",
                "html",
                "draft",
                null,
                LocalDateTime.of(2026, 6, 7, 15, 20));
    }

    private AppVersionVO versionWithFiles(List<GeneratedFileVO> files) {
        return new AppVersionVO(
                20L,
                10L,
                2,
                "demo",
                new GeneratedProjectVO("zerocode-html-app", "html", files),
                LocalDateTime.of(2026, 6, 7, 15, 21));
    }

    private DeploymentVO deployment(String id, String target, String status) {
        return new DeploymentVO(
                id,
                10L,
                2,
                "html",
                "http://platform.test/api/apps/10/versions/2/zip",
                target,
                status,
                List.of("docker build -t zerocode-app-10-v2 ."),
                List.of("Dry-run deployment only"),
                null,
                LocalDateTime.of(2026, 6, 10, 10, 0));
    }

    private Map<String, String> readZipEntries(byte[] zipBytes) throws Exception {
        Map<String, String> entries = new HashMap<>();
        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                entries.put(entry.getName(), new String(zipInputStream.readAllBytes()));
            }
        }
        return entries;
    }
}
