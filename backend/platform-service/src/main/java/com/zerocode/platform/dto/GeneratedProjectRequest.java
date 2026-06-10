package com.zerocode.platform.dto;

import com.zerocode.platform.util.ProjectSecurityLimits;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record GeneratedProjectRequest(
        @NotBlank @Size(max = ProjectSecurityLimits.MAX_PROJECT_NAME_LENGTH) String projectName,
        @Pattern(regexp = "html|vue|react") String projectType,
        @NotEmpty @Size(max = ProjectSecurityLimits.MAX_PROJECT_FILES) List<@Valid GeneratedFileRequest> files) {

    public String normalizedProjectType() {
        return projectType == null || projectType.isBlank() ? "html" : projectType;
    }
}
