package com.zerocode.platform.dto;

import com.zerocode.platform.util.ProjectSecurityLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record GeneratedFileRequest(
        @NotBlank @Size(max = ProjectSecurityLimits.MAX_FILE_PATH_LENGTH) String filePath,
        @NotBlank @Size(max = ProjectSecurityLimits.MAX_FILE_TYPE_LENGTH) String fileType,
        @NotBlank @Size(max = ProjectSecurityLimits.MAX_FILE_CONTENT_LENGTH) String content) {
}
