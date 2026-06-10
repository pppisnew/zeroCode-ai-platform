package com.zerocode.platform.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zerocode.platform.vo.GeneratedFileVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import java.io.IOException;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ProjectFileValidatorTests {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    void acceptsValidProject() {
        GeneratedProjectVO project = project(
                "zerocode-html-app",
                "html",
                List.of(
                        file("index.html", "html", "<main>Ready</main><script src=\"main.js\"></script>"),
                        file("style.css", "css", "main { color: red; }"),
                        file("main.js", "js", "setTimeout(() => console.log('ready'), 10)")));

        assertThatCode(() -> ProjectFileValidator.validateProject(project))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidProjectMetadata() {
        assertThatThrownBy(() -> ProjectFileValidator.validateProject(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project is required");
        assertThatThrownBy(() -> ProjectFileValidator.validateProject(project(" ", "html", List.of(file()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid project name");
        assertThatThrownBy(() -> ProjectFileValidator.validateProject(project("x".repeat(129), "html", List.of(file()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid project name");
        assertThatThrownBy(() -> ProjectFileValidator.validateProject(project("zerocode-app", "svelte", List.of(file()))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid project type");
    }

    @Test
    void normalizesBackslashPathsAndRejectsUnsafePaths() {
        assertThat(ProjectFileValidator.safeProjectPath("src\\App.vue"))
                .isEqualTo("src/App.vue");

        for (String path : List.of("", " ", "/index.html", "src//App.vue", "src/./App.vue", "src/../App.vue")) {
            assertThatThrownBy(() -> ProjectFileValidator.safeProjectPath(path))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Invalid file path");
        }
    }

    @Test
    void rejectsEmptyNullAndDuplicateFileLists() {
        assertThatThrownBy(() -> ProjectFileValidator.validateProjectFiles(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project files are required");
        assertThatThrownBy(() -> ProjectFileValidator.validateProjectFiles(List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Project files are required");
        assertThatThrownBy(() -> ProjectFileValidator.validateProjectFiles(List.of(
                file("src/App.vue", "vue", "<template><main /></template>"),
                file("src\\App.vue", "vue", "<template><main /></template>"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Duplicate file path");
    }

    @Test
    void rejectsDangerousHtmlContent() {
        assertRejectsContent(file("index.html", "html", "<main><script>alert(1)</script></main>"),
                "Project file must not inline scripts");
        assertRejectsContent(file("index.html", "html", "<button onclick=\"alert(1)\">Run</button>"),
                "Project file must not use inline event handlers");
        assertRejectsContent(file("index.html", "html", "<img src=https://example.com/logo.png>"),
                "Project file must not reference external URLs");
    }

    @Test
    void rejectsDangerousCssContent() {
        assertRejectsContent(file("style.css", "css", "main{background:url(//example.com/bg.png)}"),
                "Project file must not reference external URLs");
        assertRejectsContent(file("style.css", "css", "@import url('https://example.com/theme.css');"),
                "Project file must not reference external URLs");
    }

    @Test
    void rejectsDangerousScriptContent() {
        assertRejectsContent(file("src/App.tsx", "tsx", "fetch('/api/data')"),
                "Project file must not perform network requests");
        assertRejectsContent(file("src/App.vue", "vue", "new Function('return 1')"),
                "Project file must not use dynamic code execution");
        assertRejectsContent(file("main.js", "js", "setTimeout(\"alert(1)\", 10)"),
                "Project file must not use dynamic code execution");
    }

    @Test
    void matchesSharedContentSecurityFixtures() throws IOException {
        for (SecurityContentFixture fixture : securityContentFixtures()) {
            GeneratedFileVO fixtureFile = file(fixture.filePath(), fixture.fileType(), fixture.content());

            if (fixture.allowed()) {
                assertThatCode(() -> ProjectFileValidator.validateProjectFiles(List.of(fixtureFile)))
                        .as(fixture.id())
                        .doesNotThrowAnyException();
            } else {
                assertThatThrownBy(() -> ProjectFileValidator.validateProjectFiles(List.of(fixtureFile)))
                        .as(fixture.id())
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessage(fixture.expectedJavaError());
            }
        }
    }

    private static void assertRejectsContent(GeneratedFileVO file, String message) {
        assertThatThrownBy(() -> ProjectFileValidator.validateProjectFiles(List.of(file)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage(message);
    }

    private static GeneratedProjectVO project(String name, String type, List<GeneratedFileVO> files) {
        return new GeneratedProjectVO(name, type, files);
    }

    private static GeneratedFileVO file() {
        return file("index.html", "html", "<main>Ready</main>");
    }

    private static GeneratedFileVO file(String path, String type, String content) {
        return new GeneratedFileVO(path, type, content);
    }

    private static List<SecurityContentFixture> securityContentFixtures() throws IOException {
        return OBJECT_MAPPER.readValue(
                Files.readString(securityContentFixturePath()),
                new TypeReference<>() {
                });
    }

    private static Path securityContentFixturePath() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("doc/security-content-fixtures.json");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Cannot find doc/security-content-fixtures.json");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SecurityContentFixture(
            String id,
            String filePath,
            String fileType,
            String content,
            boolean allowed,
            String expectedJavaError) {
    }
}
