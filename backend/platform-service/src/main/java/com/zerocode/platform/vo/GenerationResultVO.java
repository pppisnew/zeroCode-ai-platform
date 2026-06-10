package com.zerocode.platform.vo;

public record GenerationResultVO(
        AppVO app,
        AppVersionVO version,
        GeneratedProjectVO project) {
}
