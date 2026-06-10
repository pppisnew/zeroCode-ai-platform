package com.zerocode.platform.vo;

import java.util.List;

public record GeneratedProjectVO(
        String projectName,
        String projectType,
        List<GeneratedFileVO> files) {

    public GeneratedProjectVO {
        if (projectType == null || projectType.isBlank()) {
            projectType = "html";
        }
    }
}
