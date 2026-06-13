package com.zerocode.platform.vo;

import java.time.LocalDateTime;

public record UserVO(
        Long id,
        String username,
        String role,
        LocalDateTime createTime) {
}
