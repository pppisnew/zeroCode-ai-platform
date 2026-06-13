# Phase 4: 补齐缺失功能

**日期**: 2026-06-13
**状态**: 已完成
**对应文档**: 所有 AGENTS.md 残留偏差

## 4A. 业务异常类 + 错误码（§14）

| 动作 | 文件 |
|------|------|
| NEW | `exception/BusinessException.java` — `RuntimeException` 子类，携带 `errorCode` |
| NEW | `exception/ErrorCode.java` — 枚举：AUTH_*, RES_*, VAL_*, UP_*, RATE_*, SYS_* |
| MODIFY | `config/GlobalExceptionHandler.java` — 使用 BusinessException、NotPermissionException |

## 4B. ChatMessage + AiTask 实体（§12）

| 动作 | 文件 |
|------|------|
| NEW | `model/ChatMessageEntity.java` — 映射 `chat_message` 表 |
| NEW | `model/AiTaskEntity.java` — 映射 `ai_task` 表 |
| NEW | `mapper/ChatMessageMapper.java` |
| NEW | `mapper/AiTaskMapper.java` |

## 4C. 结构化日志 + RBAC（§13, §15）

| 动作 | 文件 |
|------|------|
| NEW | `config/TraceFilter.java` — Servlet Filter，注入 `traceId` 到 MDC |
| NEW | `resources/logback-spring.xml` — `[%X{traceId}]` 格式化输出 |
| MODIFY | `config/GlobalExceptionHandler.java` — `NotPermissionException` → 403 |

## 验证

```bash
mvn test  # 42 passed, BUILD SUCCESS
```

## 最终合规状态

对照 Phase 0 开始时生成的合规矩阵：

| AGENTS.md | Phase 0 | Phase 1 | Phase 2 | Phase 3 | Phase 4 |
|-----------|---------|---------|---------|---------|---------|
| §2 核心架构 | ⚠️ | ✅ | ✅ | ✅ | ✅ |
| §3 技术栈 | ✅ | ✅ | ✅ | ✅ | ✅ |
| §4 工程标准 | ⚠️ | ✅ | ✅ | ✅ | ✅ |
| §5 AI 生成 | ✅ | — | — | — | — |
| §6 前端 | ✅ | ✅ | — | — | — |
| §7 后端 | ✅ | ✅ | — | — | — |
| §8 Agent 工作流 | ✅ | — | — | — | — |
| §9 沙箱 | ✅ | — | — | — | — |
| §10 Prompt | ✅ | — | — | — | — |
| §11 性能 | ⚠️ | — | ✅ | ✅ | — |
| §12 数据库 | — | ⚠️ | — | — | ✅ |
| §13 日志 | — | — | — | — | ✅ |
| §14 错误处理 | — | — | — | — | ✅ |
| §15 安全 | — | ✅ | ✅ | — | ✅ |
| §16 部署 | — | — | — | ✅ | ✅ |
| §17 测试 | — | — | — | — | ✅ |
| §18 AI 约束 | ✅ | — | — | — | — |
