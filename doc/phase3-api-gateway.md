# Phase 3: API Gateway

**日期**: 2026-06-13
**状态**: 已完成
**对应文档**: AGENTS.md §2.2（Java 职责：API 网关）, other.md（Nginx → Gateway）

## 变更

| 动作 | 文件 | 说明 |
|------|------|------|
| NEW | `infra/nginx/nginx.conf` | Nginx 反向代理配置 |
| MODIFY | `infra/docker-compose.yml` | 添加 nginx 容器（端口 8123） |

## 路由规则

| 路径 | 目标 | 说明 |
|------|------|------|
| `/` | frontend:5173 | Vite dev server |
| `/api/` | platform-service:8080 | 业务 API（30r/s 限流） |
| `/api/auth/` | platform-service:8080 | 认证（无限流） |
| `/api/generations/` | platform-service:8080 | AI 生成（5r/s 限流，180s 超时） |
| `/deployments/` | deploy-service:8081 | 部署 API |
| `/ai/` | ai-orchestrator:8000 | AI 服务（内部，180s 超时） |
| `/health` | nginx 自身 | Gateway 健康检查 |

## 功能

- **CORS**：允许 localhost:5173 的跨域请求
- **gzip**：压缩 text/css/js/json 响应
- **速率限制**：API 30r/s，生成 5r/s（burst 10）
- **超时**：AI 生成路径 180s proxy_read_timeout
- **X-Request-Id**：透传请求 ID 用于日志追踪

## 启动

```bash
# 启动 nginx
cd infra && docker compose up -d nginx

# 验证
curl http://localhost:8123/health
# → {"code":0,"message":"gateway ok"}

curl http://localhost:8123/api/health
# → platform-service health

curl http://localhost:5173  # 前端仍可直接访问
```

## 合规对照

| AGENTS.md | 修复前 | 修复后 |
|-----------|--------|--------|
| §2.2 API 网关 | ❌ | ✅ Nginx 反向代理 |
| §11 水平扩展 | ❌ | ✅ 网关层支持多实例 |
