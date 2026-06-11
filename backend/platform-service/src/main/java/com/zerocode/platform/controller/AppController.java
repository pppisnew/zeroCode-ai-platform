package com.zerocode.platform.controller;

import com.zerocode.platform.config.DeployServiceProperties;
import com.zerocode.platform.dto.CreateAppRequest;
import com.zerocode.platform.dto.CreateAppVersionRequest;
import com.zerocode.platform.dto.CreateDeploymentRequest;
import com.zerocode.platform.dto.DeployServiceDeploymentRequest;
import com.zerocode.platform.service.AppVersionService;
import com.zerocode.platform.service.AppService;
import com.zerocode.platform.service.DeploymentServiceClient;
import com.zerocode.platform.vo.ApiResponse;
import com.zerocode.platform.vo.AppVersionVO;
import com.zerocode.platform.vo.AppVO;
import com.zerocode.platform.vo.DeploymentVO;
import com.zerocode.platform.vo.GeneratedFileVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import com.zerocode.platform.util.DeploymentPackageBuilder;
import com.zerocode.platform.util.ProjectFileValidator;
import jakarta.validation.Valid;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/apps")
public class AppController {

    private final AppService appService;
    private final AppVersionService appVersionService;
    private final DeploymentServiceClient deploymentServiceClient;
    private final DeployServiceProperties deployServiceProperties;

    public AppController(
            AppService appService,
            AppVersionService appVersionService,
            DeploymentServiceClient deploymentServiceClient,
            DeployServiceProperties deployServiceProperties) {
        this.appService = appService;
        this.appVersionService = appVersionService;
        this.deploymentServiceClient = deploymentServiceClient;
        this.deployServiceProperties = deployServiceProperties;
    }

    @GetMapping
    public ApiResponse<List<AppVO>> listApps() {
        return ApiResponse.ok(appService.listApps());
    }

    @PostMapping
    public ApiResponse<AppVO> createApp(@Valid @RequestBody CreateAppRequest request) {
        return ApiResponse.ok(appService.createApp(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<AppVO> getApp(@PathVariable Long id) {
        return ApiResponse.ok(appService.getApp(id));
    }

    @GetMapping("/{id}/versions")
    public ApiResponse<List<AppVersionVO>> listVersions(@PathVariable Long id) {
        appService.getApp(id);
        return ApiResponse.ok(appVersionService.listVersions(id));
    }

    @GetMapping("/{id}/versions/{versionNo}/zip")
    public ResponseEntity<byte[]> exportVersionZip(
            @PathVariable Long id,
            @PathVariable Integer versionNo) {
        appService.getApp(id);
        AppVersionVO version = appVersionService.getVersion(id, versionNo);
        byte[] zipBytes = createZip(version);
        String filename = "zerocode-app-" + id + "-v" + versionNo + ".zip";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8)
                        .build()
                        .toString())
                .body(zipBytes);
    }

    @PostMapping("/{id}/versions")
    public ApiResponse<AppVersionVO> createVersion(
            @PathVariable Long id,
            @Valid @RequestBody CreateAppVersionRequest request) {
        appService.getApp(id);
        GeneratedProjectVO project = new GeneratedProjectVO(
                request.project().projectName(),
                request.project().normalizedProjectType(),
                request.project().files().stream()
                        .map(file -> new GeneratedFileVO(file.filePath(), file.fileType(), file.content()))
                        .toList());
        ProjectFileValidator.validateProject(project);
        return ApiResponse.ok(appVersionService.createVersion(id, request.prompt(), project));
    }

    @PostMapping("/{id}/versions/{versionNo}/deployments")
    public ApiResponse<DeploymentVO> createDeployment(
            @PathVariable Long id,
            @PathVariable Integer versionNo,
            @Valid @RequestBody CreateDeploymentRequest request) {
        appService.getApp(id);
        AppVersionVO version = appVersionService.getVersion(id, versionNo);
        DeployServiceDeploymentRequest deployRequest = new DeployServiceDeploymentRequest(
                id,
                versionNo,
                version.project().projectType(),
                deployServiceProperties.artifactUrl(id, versionNo),
                request.target());
        return ApiResponse.ok(deploymentServiceClient.createDeployment(deployRequest));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Boolean> deleteApp(@PathVariable Long id) {
        appService.deleteApp(id);
        return ApiResponse.ok(true);
    }

    private static final int MAX_ZIP_CONTENT_LENGTH = 200_000;

    private byte[] createZip(AppVersionVO version) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ZipOutputStream zipStream = new ZipOutputStream(outputStream, StandardCharsets.UTF_8)) {
            ProjectFileValidator.validateProjectFiles(version.project().files());
            for (GeneratedFileVO file : version.project().files()) {
                String content = file.content();
                if (content != null && content.length() > MAX_ZIP_CONTENT_LENGTH) {
                    throw new IllegalArgumentException("File content exceeds size limit");
                }
                writeZipEntry(zipStream, file);
            }
            for (GeneratedFileVO file : DeploymentPackageBuilder.buildDeploymentFiles(version.project())) {
                writeZipEntry(zipStream, file);
            }
            zipStream.finish();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to export version");
        }
    }

    private void writeZipEntry(ZipOutputStream zipStream, GeneratedFileVO file) throws IOException {
        ZipEntry entry = new ZipEntry(ProjectFileValidator.safeProjectPath(file.filePath()));
        zipStream.putNextEntry(entry);
        zipStream.write(file.content().getBytes(StandardCharsets.UTF_8));
        zipStream.closeEntry();
    }
}
