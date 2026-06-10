package com.zerocode.platform.util;

import static org.assertj.core.api.Assertions.assertThat;

import com.zerocode.platform.vo.GeneratedFileVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import java.util.List;
import org.junit.jupiter.api.Test;

class DeploymentPackageBuilderTests {

    @Test
    void buildsStaticHtmlDeploymentFiles() {
        List<GeneratedFileVO> files = DeploymentPackageBuilder.buildDeploymentFiles(project("html"));

        assertThat(files).extracting(GeneratedFileVO::filePath)
                .containsExactly("Dockerfile", "nginx.conf", "DEPLOYMENT.md");
        GeneratedFileVO dockerfile = files.getFirst();
        assertThat(dockerfile.content())
                .contains("FROM nginx:1.27-alpine")
                .contains("COPY . /usr/share/nginx/html")
                .doesNotContain("npm run build");
    }

    @Test
    void buildsViteDeploymentFilesForVueAndReact() {
        for (String projectType : List.of("vue", "react")) {
            List<GeneratedFileVO> files = DeploymentPackageBuilder.buildDeploymentFiles(project(projectType));

            GeneratedFileVO dockerfile = files.getFirst();
            assertThat(dockerfile.content())
                    .contains("FROM node:22-alpine AS build")
                    .contains("npm install --ignore-scripts --no-audit --no-fund")
                    .contains("npm run build")
                    .contains("COPY --from=build /app/dist /usr/share/nginx/html");
        }
    }

    @Test
    void skipsDeploymentFilesAlreadyProvidedByProject() {
        GeneratedProjectVO project = new GeneratedProjectVO(
                "zerocode-html-app",
                "html",
                List.of(
                        new GeneratedFileVO("index.html", "html", "<main>Ready</main>"),
                        new GeneratedFileVO("Dockerfile", "dockerfile", "FROM custom")));

        List<GeneratedFileVO> files = DeploymentPackageBuilder.buildDeploymentFiles(project);

        assertThat(files).extracting(GeneratedFileVO::filePath)
                .containsExactly("nginx.conf", "DEPLOYMENT.md");
    }

    private static GeneratedProjectVO project(String projectType) {
        return new GeneratedProjectVO(
                "zerocode-" + projectType + "-app",
                projectType,
                List.of(new GeneratedFileVO("index.html", "html", "<main>Ready</main>")));
    }
}
