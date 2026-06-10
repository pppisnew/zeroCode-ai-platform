package com.zerocode.platform.dto;

import com.zerocode.platform.vo.GeneratedProjectVO;

public record AiGenerateRequest(
        String prompt,
        Long appId,
        String projectType,
        GeneratedProjectVO baseProject) {
}
