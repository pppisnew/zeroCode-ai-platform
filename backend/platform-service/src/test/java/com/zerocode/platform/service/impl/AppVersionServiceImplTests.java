package com.zerocode.platform.service.impl;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerocode.platform.mapper.AppVersionMapper;
import com.zerocode.platform.model.AppVersionEntity;
import com.zerocode.platform.vo.GeneratedFileVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import java.util.List;
import org.junit.jupiter.api.Test;

class AppVersionServiceImplTests {

    private final AppVersionMapper appVersionMapper = mock(AppVersionMapper.class);
    private final AppVersionServiceImpl appVersionService = new AppVersionServiceImpl(
            appVersionMapper,
            new ObjectMapper());

    @Test
    void rejectsDuplicateProjectFilePathsBeforeInsert() {
        GeneratedProjectVO project = new GeneratedProjectVO(
                "zerocode-vue-app",
                "vue",
                List.of(
                        new GeneratedFileVO("src/App.vue", "vue", "one"),
                        new GeneratedFileVO("src\\App.vue", "vue", "two")));

        assertThatThrownBy(() -> appVersionService.createVersion(10L, "save", project))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate file path");
        verify(appVersionMapper, never()).insert(any(AppVersionEntity.class));
    }

    @Test
    void rejectsEmptyProjectFilesBeforeInsert() {
        GeneratedProjectVO project = new GeneratedProjectVO(
                "zerocode-html-app",
                "html",
                List.of());

        assertThatThrownBy(() -> appVersionService.createVersion(10L, "save", project))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project files are required");
        verify(appVersionMapper, never()).insert(any(AppVersionEntity.class));
    }

    @Test
    void rejectsNullProjectFilePathBeforeInsert() {
        GeneratedProjectVO project = new GeneratedProjectVO(
                "zerocode-html-app",
                "html",
                List.of(new GeneratedFileVO(null, "html", "<main>Ready</main>")));

        assertThatThrownBy(() -> appVersionService.createVersion(10L, "save", project))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid file path");
        verify(appVersionMapper, never()).insert(any(AppVersionEntity.class));
    }

    @Test
    void rejectsInvalidProjectNameBeforeInsert() {
        GeneratedProjectVO project = new GeneratedProjectVO(
                "x".repeat(129),
                "html",
                List.of(new GeneratedFileVO("index.html", "html", "<main>Ready</main>")));

        assertThatThrownBy(() -> appVersionService.createVersion(10L, "save", project))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid project name");
        verify(appVersionMapper, never()).insert(any(AppVersionEntity.class));
    }

    @Test
    void rejectsInvalidProjectTypeBeforeInsert() {
        GeneratedProjectVO project = new GeneratedProjectVO(
                "zerocode-html-app",
                "svelte",
                List.of(new GeneratedFileVO("index.html", "html", "<main>Ready</main>")));

        assertThatThrownBy(() -> appVersionService.createVersion(10L, "save", project))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid project type");
        verify(appVersionMapper, never()).insert(any(AppVersionEntity.class));
    }

    @Test
    void rejectsNetworkRequestsBeforeInsert() {
        GeneratedProjectVO project = new GeneratedProjectVO(
                "zerocode-react-app",
                "react",
                List.of(new GeneratedFileVO("src/App.tsx", "tsx", "fetch('/api/data')")));

        assertThatThrownBy(() -> appVersionService.createVersion(10L, "save", project))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project file must not perform network requests");
        verify(appVersionMapper, never()).insert(any(AppVersionEntity.class));
    }

    @Test
    void rejectsExternalCssUrlsBeforeInsert() {
        GeneratedProjectVO project = new GeneratedProjectVO(
                "zerocode-html-app",
                "html",
                List.of(new GeneratedFileVO(
                        "style.css",
                        "css",
                        "main{background-image:url(\"https://example.com/bg.png\")}")));

        assertThatThrownBy(() -> appVersionService.createVersion(10L, "save", project))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project file must not reference external URLs");
        verify(appVersionMapper, never()).insert(any(AppVersionEntity.class));
    }

    @Test
    void rejectsInlineEventHandlersBeforeInsert() {
        GeneratedProjectVO project = new GeneratedProjectVO(
                "zerocode-html-app",
                "html",
                List.of(new GeneratedFileVO(
                        "index.html",
                        "html",
                        "<main><button onclick=\"alert(1)\">Run</button></main>")));

        assertThatThrownBy(() -> appVersionService.createVersion(10L, "save", project))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project file must not use inline event handlers");
        verify(appVersionMapper, never()).insert(any(AppVersionEntity.class));
    }

    @Test
    void rejectsUnquotedExternalHtmlUrlsBeforeInsert() {
        GeneratedProjectVO project = new GeneratedProjectVO(
                "zerocode-html-app",
                "html",
                List.of(new GeneratedFileVO(
                        "index.html",
                        "html",
                        "<main><img src=https://example.com/logo.png></main>")));

        assertThatThrownBy(() -> appVersionService.createVersion(10L, "save", project))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project file must not reference external URLs");
        verify(appVersionMapper, never()).insert(any(AppVersionEntity.class));
    }
}
