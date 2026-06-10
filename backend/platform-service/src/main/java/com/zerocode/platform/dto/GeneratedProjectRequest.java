package com.zerocode.platform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GeneratedProjectRequest(
        @NotBlank @Size(max = 128) String projectName,
        @Pattern(regexp = "html|vue|react") String projectType,
        @NotEmpty @Size(max = 100) List<@Valid GeneratedFileRequest> files) {

    public String normalizedProjectType() {
        return projectType == null || projectType.isBlank() ? "html" : projectType;
    }
}
