package com.zerocode.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateAppRequest(
        @NotBlank @Size(max = 128) String appName,
        @Size(max = 1000) String description,
        @NotBlank @Size(max = 32) String type) {
}
