package com.zerocode.platform.vo;

import java.time.LocalDateTime;

public record AppVersionVO(
        Long id,
        Long appId,
        Integer versionNo,
        String prompt,
        GeneratedProjectVO project,
        LocalDateTime createTime) {
}
