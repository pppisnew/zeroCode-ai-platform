package com.zerocode.platform.util;

import com.zerocode.platform.vo.GeneratedFileVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class DeploymentPackageBuilder {
    private DeploymentPackageBuilder() {
    }

    public static List<GeneratedFileVO> buildDeploymentFiles(GeneratedProjectVO project) {
        Set<String> existingPaths = new HashSet<>();
        for (GeneratedFileVO file : project.files()) {
            existingPaths.add(ProjectFileValidator.safeProjectPath(file.filePath()));
        }

        List<GeneratedFileVO> deploymentFiles = new ArrayList<>();
        addIfAbsent(deploymentFiles, existingPaths, "Dockerfile", "dockerfile", dockerfile(project.projectType()));
        addIfAbsent(deploymentFiles, existingPaths, "nginx.conf", "conf", nginxConf());
        addIfAbsent(deploymentFiles, existingPaths, "DEPLOYMENT.md", "md", deploymentReadme(project));
        return deploymentFiles;
    }

    private static void addIfAbsent(
            List<GeneratedFileVO> files,
            Set<String> existingPaths,
            String filePath,
            String fileType,
            String content) {
        if (!existingPaths.contains(filePath)) {
            files.add(new GeneratedFileVO(filePath, fileType, content));
        }
    }

    private static String dockerfile(String projectType) {
        if ("html".equals(projectType)) {
            return """
                    FROM nginx:1.27-alpine
                    COPY . /usr/share/nginx/html
                    COPY nginx.conf /etc/nginx/conf.d/default.conf
                    EXPOSE 80
                    """;
        }
        return """
                FROM node:22-alpine AS build
                WORKDIR /app
                COPY package*.json ./
                RUN npm install --ignore-scripts --no-audit --no-fund
                COPY . .
                RUN npm run build

                FROM nginx:1.27-alpine
                COPY --from=build /app/dist /usr/share/nginx/html
                COPY nginx.conf /etc/nginx/conf.d/default.conf
                EXPOSE 80
                """;
    }

    private static String nginxConf() {
        return """
                server {
                  listen 80;
                  server_name _;
                  root /usr/share/nginx/html;
                  index index.html;

                  location / {
                    try_files $uri $uri/ /index.html;
                  }
                }
                """;
    }

    private static String deploymentReadme(GeneratedProjectVO project) {
        return """
                # Deployment

                Project: %s
                Type: %s

                Build and run locally:

                ```bash
                docker build -t zerocode-generated-app .
                docker run --rm -p 8080:80 zerocode-generated-app
                ```

                Then open:

                ```text
                http://localhost:8080
                ```
                """.formatted(project.projectName(), project.projectType());
    }
}
