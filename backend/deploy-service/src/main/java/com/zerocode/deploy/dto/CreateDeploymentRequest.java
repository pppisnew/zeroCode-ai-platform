package com.zerocode.deploy.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateDeploymentRequest(
        @NotNull @Min(1) Long appId,
        @NotNull @Min(1) Integer versionNo,
        @NotBlank @Pattern(regexp = "html|vue|react") String projectType,
        @NotBlank @Size(max = 500) String artifactUrl,
        @NotBlank @Pattern(regexp = "docker|github-actions|kubernetes") String target) {
}
