package com.zerocode.platform.vo;

import java.time.LocalDateTime;

public record AppVO(
        Long id,
        Long userId,
        String appName,
        String description,
        String type,
        String status,
        String deployUrl,
        LocalDateTime createTime) {
}
