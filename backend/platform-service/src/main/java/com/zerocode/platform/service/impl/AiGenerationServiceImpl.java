package com.zerocode.platform.service.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerocode.platform.dto.AiGenerateRequest;
import com.zerocode.platform.dto.CreateAppRequest;
import com.zerocode.platform.dto.GenerateHtmlRequest;
import com.zerocode.platform.service.AiGenerationService;
import com.zerocode.platform.service.AppService;
import com.zerocode.platform.service.AppVersionService;
import com.zerocode.platform.vo.ApiResponse;
import com.zerocode.platform.vo.AppVO;
import com.zerocode.platform.vo.AppVersionVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import com.zerocode.platform.vo.GenerationResultVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class AiGenerationServiceImpl implements AiGenerationService {

    private static final TypeReference<ApiResponse<GeneratedProjectVO>> RESPONSE_TYPE =
            new TypeReference<>() {
            };

    private final RestClient aiRestClient;
    private final ObjectMapper objectMapper;
    private final AppService appService;
    private final AppVersionService appVersionService;

    public AiGenerationServiceImpl(
            @Qualifier("aiRestClient") RestClient aiRestClient,
            ObjectMapper objectMapper,
            AppService appService,
            AppVersionService appVersionService) {
        this.aiRestClient = aiRestClient;
        this.objectMapper = objectMapper;
        this.appService = appService;
        this.appVersionService = appVersionService;
    }

    @Override
    public GenerationResultVO generateHtml(GenerateHtmlRequest request) {
        AiGenerateRequest aiRequest = buildAiRequest(request);
        Object response = aiRestClient.post()
                .uri("/generations/html")
                .body(aiRequest)
                .retrieve()
                .body(Object.class);

        ApiResponse<GeneratedProjectVO> apiResponse = objectMapper.convertValue(response, RESPONSE_TYPE);
        if (apiResponse == null || apiResponse.code() != 0) {
            String message = apiResponse == null ? "AI service unavailable" : apiResponse.message();
            throw new IllegalArgumentException(message);
        }
        GeneratedProjectVO project = apiResponse.data();
        AppVO app = resolveApp(request, project);
        AppVersionVO version = appVersionService.createVersion(app.id(), request.prompt(), project);
        return new GenerationResultVO(app, version, project);
    }

    private AiGenerateRequest buildAiRequest(GenerateHtmlRequest request) {
        GeneratedProjectVO baseProject = null;
        String projectType = request.normalizedProjectType();
        if (request.appId() != null) {
            AppVersionVO latestVersion = appVersionService.getLatestVersion(request.appId());
            if (latestVersion != null) {
                baseProject = latestVersion.project();
                projectType = baseProject.projectType();
            }
        }
        return new AiGenerateRequest(request.prompt(), request.appId(), projectType, baseProject);
    }

    private AppVO resolveApp(GenerateHtmlRequest request, GeneratedProjectVO project) {
        if (request.appId() != null) {
            return appService.getApp(request.appId());
        }
        return appService.createGeneratedApp(
                project.projectName(),
                request.prompt(),
                request.normalizedProjectType());
    }
}
