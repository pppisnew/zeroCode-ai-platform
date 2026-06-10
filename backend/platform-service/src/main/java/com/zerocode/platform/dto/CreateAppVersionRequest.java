package com.zerocode.platform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateAppVersionRequest(
        @NotBlank @Size(max = 4000) String prompt,
        @NotNull @Valid GeneratedProjectRequest project) {
}
