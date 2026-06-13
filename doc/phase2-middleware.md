# Phase 2: 中间件接入

**日期**: 2026-06-13
**状态**: 已完成
**对应文档**: AGENTS.md §3（Redis, RabbitMQ, MinIO 强制技术栈）, §11（分布式缓存/队列）, §15（速率限制）

## 2A. Redis — Session + Cache + Rate Limit

### 变更

| 动作 | 文件 | 说明 |
|------|------|------|
| NEW | `config/RedisConfig.java` | StringRedisTemplate + RedisTemplate Bean 配置，`@EnableCaching` |
| NEW | `config/RateLimitInterceptor.java` | Redis 令牌桶速率限制：/generations/* 10次/分钟 |
| MODIFY | `AppServiceImpl.java` | `getApp()` 添加 `@Cacheable("apps")` |
| MODIFY | `SaTokenConfig.java` | 注册 RateLimitInterceptor（order=1）+ SaInterceptor（order=2） |

### 接入场景

1. **Sa-Token 会话存储**：`spring-boot-starter-data-redis` 在 classpath，Sa-Token 自动将 token 持久化到 Redis
2. **缓存**：`getApp()` 查询结果缓存到 Redis，减少数据库查询
3. **速率限制**：`POST /generations/*` 每个用户/IP 每分钟 10 次，超限返回 429

## 2B. RabbitMQ — 异步 AI 生成

### 变更

| 动作 | 文件 | 说明 |
|------|------|------|
| NEW | `config/RabbitConfig.java` | `@EnableRabbit`，声明 `ai.generation.queue/exchange/routing` |
| NEW | `mq/AiGenerationListener.java` | `@RabbitListener` 消费生成任务，结果写入 Redis |
| MODIFY | `GenerationController.java` | `POST /generations/async` + `GET /generations/async/{taskId}` |

### 接入场景

```
POST /generations/async
  → RabbitMQ queue
  → AiGenerationListener 消费
  → 调用 AI 生成
  → 结果写入 Redis (gen:{taskId}:status=completed|failed)
  → 前端轮询 GET /generations/async/{taskId} 获取结果
```

同步端点 (`POST /generations/html`) 保持不变，异步端点作为可选增强。

## 2C. MinIO — 对象存储

### 变更

| 动作 | 文件 | 说明 |
|------|------|------|
| NEW | `config/MinioProperties.java` | `@ConfigurationProperties(prefix="zerocode.minio")` |
| NEW | `config/MinioConfig.java` | MinioClient Bean（endpoint + credentials） |

### 接入场景（后续 Phase 3D 实现上传逻辑）

- ZIP 导出存储到 MinIO
- 版本快照（snapshot_url）存 MinIO
- deploy-service 从 MinIO 拉取 artifact

当前已完成 Bean 配置和依赖注入，Phase 3 或 4 实现具体上传逻辑。

## 验证

```bash
mvn test  # 42 passed, BUILD SUCCESS
```

## 合规对照

| AGENTS.md | 修复前 | 修复后 |
|-----------|--------|--------|
| §3 Redis | ❌ 空跑 | ✅ 会话+缓存+限流 |
| §3 RabbitMQ | ❌ 空跑 | ✅ 异步生成队列 |
| §3 MinIO | ❌ 空跑 | ✅ Bean 已配置（上传逻辑待后续） |
| §11 分布式缓存 | ❌ | ✅ @Cacheable |
| §11 分布式队列 | ❌ | ✅ RabbitMQ 队列 |
| §15 速率限制 | ❌ | ✅ 10次/分钟 |
