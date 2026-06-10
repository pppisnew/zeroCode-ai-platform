package com.zerocode.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerocode.platform.dto.GenerateHtmlRequest;
import com.zerocode.platform.vo.ApiResponse;
import com.zerocode.platform.vo.AppVO;
import com.zerocode.platform.vo.AppVersionVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class PlatformServiceApplicationTests {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void serializesLongIdsAsStringsForFrontendSafety() throws Exception {
        AppVO app = new AppVO(
                2063178929686544385L,
                1L,
                "zerocode-html-app",
                "demo",
                "html",
                "draft",
                null,
                LocalDateTime.of(2026, 6, 6, 16, 0));
        AppVersionVO version = new AppVersionVO(
                2063179085651738625L,
                2063178929686544385L,
                2,
                "update",
                new GeneratedProjectVO("demo", "html", List.of()),
                LocalDateTime.of(2026, 6, 6, 16, 1));

        JsonNode payload = objectMapper.valueToTree(ApiResponse.ok(List.of(app, version)));

        assertThat(payload.get("data").get(0).get("id").isTextual()).isTrue();
        assertThat(payload.get("data").get(0).get("userId").isTextual()).isTrue();
        assertThat(payload.get("data").get(1).get("appId").isTextual()).isTrue();
    }

    @Test
    void defaultsProjectTypeToHtml() {
        GenerateHtmlRequest request = new GenerateHtmlRequest("prompt", null, null);

        assertThat(request.normalizedProjectType()).isEqualTo("html");
    }

    @Test
    void keepsExplicitProjectType() {
        GenerateHtmlRequest request = new GenerateHtmlRequest("prompt", null, "vue");

        assertThat(request.normalizedProjectType()).isEqualTo("vue");
    }

    @Test
    void defaultsGeneratedProjectTypeForOldSnapshots() {
        GeneratedProjectVO project = new GeneratedProjectVO("demo", null, List.of());

        assertThat(project.projectType()).isEqualTo("html");
    }
}
