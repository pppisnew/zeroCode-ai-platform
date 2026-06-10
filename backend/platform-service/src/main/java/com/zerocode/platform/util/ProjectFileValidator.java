package com.zerocode.platform.util;

import com.zerocode.platform.vo.GeneratedFileVO;
import com.zerocode.platform.vo.GeneratedProjectVO;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public final class ProjectFileValidator {
    private static final Set<String> PROJECT_TYPES = Set.of("html", "vue", "react");
    private static final Pattern NETWORK_SCRIPT_PATTERN = Pattern.compile(
            "\\b(fetch|XMLHttpRequest|WebSocket|EventSource)\\s*\\(",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DANGEROUS_SCRIPT_PATTERN = Pattern.compile(
            "\\beval\\s*\\(|\\bnew\\s+Function\\s*\\(|\\bset(?:Timeout|Interval)\\s*\\(\\s*['\"]");
    private static final Pattern EXTERNAL_CSS_URL_PATTERN = Pattern.compile(
            "(?:@import\\s+)?url\\(\\s*['\"]?(?:https?:)?//",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern EXTERNAL_HTML_URL_PATTERN = Pattern.compile(
            "\\s(?:src|href|action|poster)\\s*=\\s*(?:['\"]\\s*)?(?:https?:)?//",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern INLINE_EVENT_HANDLER_PATTERN = Pattern.compile(
            "\\son[a-z]+\\s*=",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_TAG_PATTERN = Pattern.compile(
            "<script\\b([^>]*)>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SCRIPT_SRC_PATTERN = Pattern.compile(
            "\\ssrc\\s*=",
            Pattern.CASE_INSENSITIVE);

    private ProjectFileValidator() {
    }

    public static void validateUniqueSafePaths(List<String> filePaths) {
        if (filePaths == null || filePaths.isEmpty()) {
            throw new IllegalArgumentException("Project files are required");
        }
        Set<String> seenPaths = new HashSet<>();
        for (String filePath : filePaths) {
            String normalized = safeProjectPath(filePath);
            if (!seenPaths.add(normalized)) {
                throw new IllegalArgumentException("Duplicate file path");
            }
        }
    }

    public static void validateProject(GeneratedProjectVO project) {
        if (project == null) {
            throw new IllegalArgumentException("Project is required");
        }
        if (project.projectName() == null || project.projectName().isBlank()
                || project.projectName().length() > ProjectSecurityLimits.MAX_PROJECT_NAME_LENGTH) {
            throw new IllegalArgumentException("Invalid project name");
        }
        if (!PROJECT_TYPES.contains(project.projectType())) {
            throw new IllegalArgumentException("Invalid project type");
        }
        validateProjectFiles(project.files());
    }

    public static void validateProjectFiles(List<GeneratedFileVO> files) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("Project files are required");
        }
        Set<String> seenPaths = new HashSet<>();
        for (GeneratedFileVO file : files) {
            if (file == null) {
                throw new IllegalArgumentException("Project files are required");
            }
            String normalizedPath = safeProjectPath(file.filePath());
            if (!seenPaths.add(normalizedPath)) {
                throw new IllegalArgumentException("Duplicate file path");
            }
            validateFileContent(file, normalizedPath);
        }
    }

    public static String safeProjectPath(String filePath) {
        if (filePath == null) {
            throw new IllegalArgumentException("Invalid file path");
        }
        String normalized = filePath.replace('\\', '/');
        if (normalized.startsWith("/")
                || normalized.isBlank()) {
            throw new IllegalArgumentException("Invalid file path");
        }
        for (String segment : normalized.split("/")) {
            if (segment.equals(".") || segment.equals("..") || segment.isBlank()) {
                throw new IllegalArgumentException("Invalid file path");
            }
        }
        return normalized;
    }

    private static void validateFileContent(GeneratedFileVO file, String filePath) {
        String fileType = file.fileType() == null ? "" : file.fileType().toLowerCase();
        String content = file.content() == null ? "" : file.content();
        if (isHtmlFile(filePath, fileType)) {
            validateHtmlContent(content);
        }
        if (isStyleFile(filePath, fileType) && EXTERNAL_CSS_URL_PATTERN.matcher(content).find()) {
            throw new IllegalArgumentException("Project file must not reference external URLs");
        }
        if (isScriptFile(filePath, fileType)) {
            if (NETWORK_SCRIPT_PATTERN.matcher(content).find()) {
                throw new IllegalArgumentException("Project file must not perform network requests");
            }
            if (DANGEROUS_SCRIPT_PATTERN.matcher(content).find()) {
                throw new IllegalArgumentException("Project file must not use dynamic code execution");
            }
        }
    }

    private static void validateHtmlContent(String content) {
        var scriptMatcher = SCRIPT_TAG_PATTERN.matcher(content);
        while (scriptMatcher.find()) {
            if (!SCRIPT_SRC_PATTERN.matcher(scriptMatcher.group(1)).find()) {
                throw new IllegalArgumentException("Project file must not inline scripts");
            }
        }
        if (INLINE_EVENT_HANDLER_PATTERN.matcher(content).find()) {
            throw new IllegalArgumentException("Project file must not use inline event handlers");
        }
        if (EXTERNAL_HTML_URL_PATTERN.matcher(content).find()) {
            throw new IllegalArgumentException("Project file must not reference external URLs");
        }
    }

    private static boolean isHtmlFile(String filePath, String fileType) {
        return fileType.equals("html") || filePath.endsWith(".html");
    }

    private static boolean isStyleFile(String filePath, String fileType) {
        return fileType.equals("css") || filePath.endsWith(".css");
    }

    private static boolean isScriptFile(String filePath, String fileType) {
        return Set.of("js", "jsx", "ts", "tsx", "vue").contains(fileType)
                || filePath.endsWith(".js")
                || filePath.endsWith(".jsx")
                || filePath.endsWith(".ts")
                || filePath.endsWith(".tsx")
                || filePath.endsWith(".vue");
    }
}
