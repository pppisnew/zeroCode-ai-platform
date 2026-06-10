package com.zerocode.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record GenerateHtmlRequest(
        @NotBlank @Size(max = 4000) String prompt,
        Long appId,
        @Pattern(regexp = "html|vue|react") String projectType) {

    public String normalizedProjectType() {
        return projectType == null || projectType.isBlank() ? "html" : projectType;
    }
}
