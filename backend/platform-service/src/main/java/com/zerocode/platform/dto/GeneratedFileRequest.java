package com.zerocode.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeneratedFileRequest(
        @NotBlank @Size(max = 500) String filePath,
        @NotBlank @Size(max = 32) String fileType,
        @NotBlank @Size(max = 200_000) String content) {
}
