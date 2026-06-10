# Operations Runbook

## 1. 目标

本文档作为 ZeroCode AI Platform 后期运维入口，覆盖本地启动、环境变量、健康检查、测试验证、部署执行器、安全边界、数据位置和常见故障排查。

恢复任务上下文时优先阅读：

- `doc/task-current.md`
- `doc/operations.md`
- `doc/deployment.md`
- `doc/security-rules.md`
- `doc/git.md`

## 2. 服务拓扑

| 服务 | 路径 | 默认端口 | 说明 |
| --- | --- | --- | --- |
| Frontend | `frontend` | `5173` | Vue 3 + Vite 工作台 |
| AI Orchestrator | `ai-services/ai-orchestrator` | `8000` | FastAPI AI 生成、修复、sandbox |
| Platform Service | `backend/platform-service` | `8080`，context path `/api` | 应用、版本、ZIP、部署入口 |
| Deploy Service | `backend/deploy-service` | `8081` | 部署记录、executor routing、受控部署执行器 |
| MySQL | `infra/docker-compose.yml` | `3306` | 平台业务数据 |
| Redis | `infra/docker-compose.yml` | `6379` | 后续缓存/会话能力 |
| RabbitMQ | `infra/docker-compose.yml` | `5672` / `15672` | 后续异步任务能力 |
| MinIO | `infra/docker-compose.yml` | `9000` / `9001` | 后续对象存储能力 |

## 3. 启动顺序

推荐优先使用脚本启动完整本地环境：

```bash
scripts/start-project.sh
```

脚本会依次启动：

- `infra` Docker Compose 基础设施。
- Python AI Orchestrator。
- Deploy Service。
- Platform Service。
- Frontend。

运行时文件：

- PID：`.runtime/pids/*.pid`
- 日志：`.runtime/logs/*.log`

如果只想启动应用服务，不启动 Docker 基础设施：

```bash
scripts/start-project.sh --skip-infra
```

如果不希望脚本自动执行缺失依赖安装：

```bash
scripts/start-project.sh --no-install
```

停止应用服务：

```bash
scripts/stop-project.sh
```

停止应用服务并关闭 Docker 基础设施：

```bash
scripts/stop-project.sh --infra
```

停止并删除 Docker 数据卷：

```bash
scripts/stop-project.sh --volumes
```

注意：`--volumes` 会删除本地 MySQL、Redis、RabbitMQ、MinIO 数据卷，只能在确认不需要本地数据后使用。

### 3.1 基础设施

以下为手动启动方式。

```bash
cd infra
docker compose up -d
```

检查容器：

```bash
docker compose ps
```

停止基础设施：

```bash
cd infra
docker compose down
```

如需删除本地数据卷，必须先确认数据已备份，再执行：

```bash
cd infra
docker compose down -v
```

### 3.2 Python AI Orchestrator

```bash
cd ai-services/ai-orchestrator
UV_CACHE_DIR=/private/tmp/uv-cache uv sync
UV_CACHE_DIR=/private/tmp/uv-cache uv run uvicorn app.main:app --reload --port 8000
```

健康检查：

```bash
curl http://localhost:8000/health
```

### 3.3 Deploy Service

```bash
cd backend/deploy-service
mvn spring-boot:run
```

当前 deploy-service 没有独立 health endpoint。最小可用性检查：

```bash
curl http://localhost:8081/deployments/not-found
```

预期返回统一 envelope 或明确的 404/错误响应，而不是连接失败。

### 3.4 Platform Service

```bash
cd backend/platform-service
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/api/health
```

### 3.5 Frontend

```bash
cd frontend
npm run dev
```

访问：

```text
http://localhost:5173
```

## 4. 关键环境变量

### 4.1 Platform Service

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `MYSQL_HOST` | `localhost` | MySQL host |
| `MYSQL_PORT` | `3306` | MySQL port |
| `MYSQL_DATABASE` | `zerocode` | MySQL database |
| `MYSQL_USERNAME` | `zerocode` | MySQL 用户 |
| `MYSQL_PASSWORD` | `zerocode` | MySQL 密码 |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `RABBITMQ_HOST` | `localhost` | RabbitMQ host |
| `RABBITMQ_PORT` | `5672` | RabbitMQ port |
| `RABBITMQ_USERNAME` | `zerocode` | RabbitMQ 用户 |
| `RABBITMQ_PASSWORD` | `zerocode` | RabbitMQ 密码 |
| `AI_SERVICE_BASE_URL` | `http://localhost:8000` | Python AI 服务地址 |
| `DEPLOY_SERVICE_BASE_URL` | `http://localhost:8081` | Deploy service 地址 |
| `PLATFORM_ARTIFACT_BASE_URL` | `http://localhost:8080/api` | 部署 artifact ZIP 回调基址 |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO endpoint |
| `MINIO_ACCESS_KEY` | `zerocode` | MinIO access key |
| `MINIO_SECRET_KEY` | `zerocode123` | MinIO secret key |
| `MINIO_BUCKET` | `zerocode` | MinIO bucket |

### 4.2 Python AI Orchestrator

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `ZEROCODE_ENABLE_BROWSER_SANDBOX` | 未启用 | `true` 时启用 Playwright browser sandbox |
| `ZEROCODE_ENABLE_DOCKER_SANDBOX` | 未启用 | `true` 时对 Vue/React 执行 Docker sandbox 构建检查 |
| `ZEROCODE_DOCKER_SANDBOX_IMAGE` | `node:22-alpine` | Docker sandbox 镜像 |

### 4.3 Deploy Service

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DEPLOY_STORE_PATH` | `/tmp/zerocode-deployments.json` | 部署记录 JSON 存储路径 |
| `DEPLOY_DOCKER_EXECUTOR_ENABLED` | `false` | 是否注册 Docker executor |
| `DEPLOY_DOCKER_EXECUTION_MODE` | `dry-run` | `real` 时才执行 Docker 命令 |
| `DEPLOY_DOCKER_WORKSPACE_ROOT` | `/tmp/zerocode-docker-deployments` | Docker artifact 解压根目录 |
| `DEPLOY_DOCKER_COMMAND_TIMEOUT_SECONDS` | `300` | Docker 单命令超时 |
| `DEPLOY_DOCKER_IMAGE_REPOSITORY_PREFIX` | `zerocode` | Docker 镜像名前缀 |
| `DEPLOY_DOCKER_PUSH_ENABLED` | `false` | 是否执行 `docker push` |
| `DEPLOY_GITHUB_ACTIONS_EXECUTOR_ENABLED` | `false` | 是否注册 GitHub Actions executor |
| `DEPLOY_GITHUB_ACTIONS_EXECUTION_MODE` | `dry-run` | `real` 时才调用 GitHub API |
| `DEPLOY_GITHUB_ACTIONS_TOKEN` | 空 | GitHub token，不应写入日志 |
| `DEPLOY_GITHUB_ACTIONS_OWNER` | 空 | GitHub 仓库 owner |
| `DEPLOY_GITHUB_ACTIONS_REPO` | 空 | GitHub 仓库名 |
| `DEPLOY_GITHUB_ACTIONS_WORKFLOW_ID` | 空 | workflow 文件名或 id |
| `DEPLOY_GITHUB_ACTIONS_REF` | `main` | workflow dispatch ref |
| `DEPLOY_KUBERNETES_EXECUTOR_ENABLED` | `false` | 是否注册 Kubernetes executor |
| `DEPLOY_KUBERNETES_EXECUTION_MODE` | `dry-run` | `real` 时才执行 `kubectl` |
| `DEPLOY_KUBERNETES_NAMESPACE` | `default` | Kubernetes namespace |
| `DEPLOY_KUBERNETES_KUBECTL_BINARY` | `kubectl` | kubectl 可执行文件 |
| `DEPLOY_KUBERNETES_KUBECONFIG` | 空 | 可选 kubeconfig 路径，不写入日志 |
| `DEPLOY_KUBERNETES_IMAGE_REPOSITORY_PREFIX` | `zerocode` | 镜像名前缀 |
| `DEPLOY_KUBERNETES_SERVICE_PORT` | `80` | Service 端口 |

## 5. 验证命令

提交前建议执行完整验证：

```bash
cd frontend
npm run test
npm run build
```

```bash
cd ai-services/ai-orchestrator
UV_CACHE_DIR=/private/tmp/uv-cache uv run pytest
UV_CACHE_DIR=/private/tmp/uv-cache uv run ruff check .
```

```bash
cd backend/platform-service
mvn -Dmaven.repo.local=/private/tmp/zerocode-m2 test
```

```bash
cd backend/deploy-service
mvn -Dmaven.repo.local=/private/tmp/zerocode-m2 test
```

## 6. 部署执行器安全边界

Deploy service 默认不会执行真实生产命令。

- 未启用目标 executor：使用 dry-run fallback，返回 `planned`。
- executor 已启用但 `execution-mode` 不是 `real`：返回 `skipped`。
- 只有同时满足 `enabled=true` 和 `execution-mode=real`，才进入真实执行路径。

生产环境启用真实执行前必须确认：

- Docker daemon、镜像仓库和 push 权限可用。
- GitHub token 权限最小化，workflow id/ref 配置正确。
- Kubernetes kubeconfig 权限最小化，namespace 和镜像来源明确。
- `PLATFORM_ARTIFACT_BASE_URL` 能被 deploy-service 或外部执行器访问。
- 真实执行日志不能包含 token、kubeconfig、密码等敏感信息。

详细规则见 `doc/deployment.md`。

## 7. 数据与备份

本地开发默认数据位置：

- MySQL：Docker volume `infra_mysql-data`
- Redis：Docker volume `infra_redis-data`
- RabbitMQ：Docker volume `infra_rabbitmq-data`
- MinIO：Docker volume `infra_minio-data`
- Deploy records：`/tmp/zerocode-deployments.json`
- Docker deploy workspace：`/tmp/zerocode-docker-deployments`
- Maven cache：`/private/tmp/zerocode-m2`
- uv cache：`/private/tmp/uv-cache`

备份建议：

- MySQL 业务数据以 `mysqldump` 为准。
- MinIO 数据按 bucket 做对象级备份。
- deploy-service 本地 JSON 只适合开发和演示，生产应迁移到数据库。
- `/tmp` 下缓存和 workspace 不应作为长期数据来源。

## 8. 日志与排障

### AI 服务不可用

现象：

- 前端生成失败。
- platform-service 返回 `Upstream service unavailable`。

检查：

```bash
curl http://localhost:8000/health
```

处理：

- 确认 `AI_SERVICE_BASE_URL` 指向正确。
- 确认 Python 服务已启动。
- 执行 `UV_CACHE_DIR=/private/tmp/uv-cache uv run pytest` 排除本地依赖问题。

### 数据库连接失败

现象：

- platform-service 启动失败或接口报数据库连接错误。

检查：

```bash
cd infra
docker compose ps
```

处理：

- 确认 MySQL 容器健康。
- 确认 `MYSQL_HOST`、`MYSQL_PORT`、`MYSQL_DATABASE`、`MYSQL_USERNAME`、`MYSQL_PASSWORD`。
- 首次初始化 SQL 位于 `infra/mysql/init.sql`，对照文档见 `doc/sql.md`。

### 部署请求一直是 planned 或 skipped

原因：

- 默认 dry-run 行为正常。
- 目标 executor 未启用，或 `execution-mode` 不是 `real`。
- GitHub Actions 配置不完整时会跳过。

处理：

- 先阅读 `doc/deployment.md` 的 executor 配置。
- 检查 `DEPLOY_*_EXECUTOR_ENABLED` 和 `DEPLOY_*_EXECUTION_MODE`。
- 检查 deploy-service executionLogs。

### Docker sandbox 或 Docker executor 不可用

检查：

```bash
docker info
```

处理：

- Python Docker sandbox 默认跳过，不应阻塞普通测试。
- 真实 Docker executor 需要 Docker daemon 可访问。
- 当前环境若访问 Docker socket 被沙箱限制，需要在具备权限的环境中验证。

### Playwright browser sandbox skipped

说明：

- `ZEROCODE_ENABLE_BROWSER_SANDBOX` 未启用时跳过是正常行为。
- 本机浏览器不可用时会返回 `Browser sandbox: skipped (...)`，普通测试不应失败。

处理：

- 需要真实浏览器验证时，安装 Playwright 浏览器并设置 `ZEROCODE_ENABLE_BROWSER_SANDBOX=true`。

## 9. 变更流程

每次改动后：

1. 更新 `doc/task-current.md`。
2. 若改动安全规则，更新 `doc/security-rules.md` 和 `doc/security-content-fixtures.json`。
3. 若改动部署配置，更新 `doc/deployment.md` 和本文档。
4. 执行受影响测试；发布前执行第 5 节完整验证。
5. 执行 `git status --short`，确认没有 `node_modules`、`dist`、`target`、`.venv`、`.env` 进入暂存区。
6. 提交并推送。

推荐提交前检查：

```bash
git diff --check
git status --short
```
