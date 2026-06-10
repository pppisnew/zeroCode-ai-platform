package com.zerocode.platform.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerocode.platform.dto.GenerateHtmlRequest;
import com.zerocode.platform.service.AppService;
import com.zerocode.platform.service.AppVersionService;
import com.zerocode.platform.vo.AppVO;
import com.zerocode.platform.vo.AppVersionVO;
import com.zerocode.platform.vo.GeneratedFileVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import com.zerocode.platform.vo.GenerationResultVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AiGenerationServiceImplTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AppService appService = mock(AppService.class);
    private final AppVersionService appVersionService = mock(AppVersionService.class);

    @Test
    void sendsLatestVersionAsBaseProjectForConversationalGeneration() {
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://ai.test");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        AiGenerationServiceImpl service = new AiGenerationServiceImpl(
                restClientBuilder.build(),
                objectMapper,
                appService,
                appVersionService);

        GeneratedProjectVO baseProject = new GeneratedProjectVO(
                "zerocode-vue-app",
                "vue",
                List.of(new GeneratedFileVO("src/App.vue", "vue", "<template></template>")));
        GeneratedProjectVO generatedProject = new GeneratedProjectVO(
                "zerocode-vue-app",
                "vue",
                List.of(new GeneratedFileVO("src/App.vue", "vue", "<template><main /></template>")));
        AppVO app = new AppVO(
                10L,
                1L,
                "zerocode-vue-app",
                "base prompt",
                "vue",
                "draft",
                null,
                LocalDateTime.of(2026, 6, 7, 14, 0));
        AppVersionVO latestVersion = new AppVersionVO(
                20L,
                10L,
                1,
                "base prompt",
                baseProject,
                LocalDateTime.of(2026, 6, 7, 14, 1));
        AppVersionVO savedVersion = new AppVersionVO(
                21L,
                10L,
                2,
                "增加统计卡片",
                generatedProject,
                LocalDateTime.of(2026, 6, 7, 14, 2));

        when(appVersionService.getLatestVersion(10L)).thenReturn(latestVersion);
        when(appService.getApp(10L)).thenReturn(app);
        when(appVersionService.createVersion(10L, "增加统计卡片", generatedProject))
                .thenReturn(savedVersion);
        server.expect(requestTo("http://ai.test/generations/html"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {
                          "prompt": "增加统计卡片",
                          "appId": 10,
                          "projectType": "vue",
                          "baseProject": {
                            "projectName": "zerocode-vue-app",
                            "projectType": "vue",
                            "files": [
                              {
                                "filePath": "src/App.vue",
                                "fileType": "vue",
                                "content": "<template></template>"
                              }
                            ]
                          }
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "code": 0,
                          "message": "ok",
                          "data": {
                            "projectName": "zerocode-vue-app",
                            "projectType": "vue",
                            "files": [
                              {
                                "filePath": "src/App.vue",
                                "fileType": "vue",
                                "content": "<template><main /></template>"
                              }
                            ]
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        GenerationResultVO result = service.generateHtml(
                new GenerateHtmlRequest("增加统计卡片", 10L, "html"));

        assertThat(result.project().projectType()).isEqualTo("vue");
        assertThat(result.version().versionNo()).isEqualTo(2);
        verify(appVersionService).getLatestVersion(10L);
        verify(appService).getApp(10L);
        verify(appVersionService).createVersion(10L, "增加统计卡片", generatedProject);
        server.verify();
    }
}
