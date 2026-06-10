package com.zerocode.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CreateDeploymentRequest(
        @NotBlank @Pattern(regexp = "docker|github-actions|kubernetes") String target) {
}
