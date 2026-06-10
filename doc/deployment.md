# Deployment

## 1. 当前目标

Phase 3 的部署链路先实现最小可验证能力：

- ZIP 导出时附加可直接 Docker 部署的文件。
- 支持 HTML 静态项目。
- 支持 Vue/React Vite 项目。
- 不在平台服务内直接执行用户项目构建或部署命令。

当前已新增 `deploy-service`、executor routing 边界和 Docker/GitHub Actions/Kubernetes 三类受控执行器。默认仍为 dry-run 或禁用状态，不执行真实命令；只有在对应 executor 显式启用且 `execution-mode=real` 时，才进入受控真实执行路径。

## 2. 当前实现

Java 平台服务 ZIP 导出会追加部署文件。

入口：

- `backend/platform-service/src/main/java/com/zerocode/platform/controller/AppController.java`

部署包生成器：

- `backend/platform-service/src/main/java/com/zerocode/platform/util/DeploymentPackageBuilder.java`

测试：

- `backend/platform-service/src/test/java/com/zerocode/platform/util/DeploymentPackageBuilderTests.java`
- `backend/platform-service/src/test/java/com/zerocode/platform/controller/AppControllerTests.java`

Deploy service：

- `backend/deploy-service`
- `POST /deployments`
- `GET /deployments/{id}`

## 3. ZIP 导出内容

除项目原始文件外，导出 ZIP 会追加：

- `Dockerfile`
- `nginx.conf`
- `DEPLOYMENT.md`

如果项目本身已经包含同名文件，平台不会覆盖用户文件。

## 4. Dockerfile 策略

### HTML 项目

HTML 项目直接使用 Nginx 托管当前目录：

```dockerfile
FROM nginx:1.27-alpine
COPY . /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

### Vue/React 项目

Vue/React 项目使用 Node 构建，再用 Nginx 托管 `dist`：

```dockerfile
FROM node:22-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm install --ignore-scripts --no-audit --no-fund
COPY . .
RUN npm run build

FROM nginx:1.27-alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

## 5. 本地运行

导出并解压 ZIP 后：

```bash
docker build -t zerocode-generated-app .
docker run --rm -p 8080:80 zerocode-generated-app
```

访问：

```text
http://localhost:8080
```

## 6. 安全边界

当前平台服务只生成部署文件，不直接执行部署命令。

安全约束：

- ZIP 导出前仍执行 `ProjectFileValidator.validateProjectFiles()`。
- 文件路径必须通过 `ProjectFileValidator.safeProjectPath()`。
- 部署包生成器不覆盖用户已有部署文件。
- Vue/React 构建验证由 Python Docker sandbox 可选执行。

## 7. 当前 deploy-service 边界

当前 `deploy-service` 负责：

- 接收部署请求。
- 校验 appId、versionNo、projectType、artifactUrl、target。
- 创建部署记录。
- 将部署记录持久化到本地 JSON 文件。
- 按执行器结果返回 `planned`、`skipped`、`succeeded` 或 `failed` 状态。
- 返回建议命令。
- 通过 dry-run 或目标执行器生成执行日志。
- 使用统一 envelope。

当前持久化：

- 模型：`DeploymentRecord`
- 接口：`DeploymentRepository`
- 实现：`FileDeploymentRepository`
- 默认路径：`/tmp/zerocode-deployments.json`
- 配置项：`zerocode.deploy.store-path`
- 环境变量：`DEPLOY_STORE_PATH`

后续可替换为 MySQL/MyBatis 或其他生产级存储。

当前执行器：

- 接口：`DeploymentExecutor`
- fallback 实现：`DryRunDeploymentExecutor`
- 目标实现：`DockerDeploymentExecutor`、`GithubActionsDeploymentExecutor`、`KubernetesDeploymentExecutor`
- 默认状态：未显式启用目标执行器时返回 `planned`
- 显式启用但未进入 real mode：返回 `skipped`
- real mode 成功：返回 `succeeded`
- real mode 失败：返回 `failed`
- 日志：记录 target、artifact、planned command 和执行摘要；敏感凭据不写入日志
- 安全边界：默认不执行真实 Docker、GitHub Actions、Kubernetes 命令；真实执行必须显式配置启用

执行器路由：

- 路由器：`DeploymentExecutorRouter`
- 匹配规则：优先选择支持指定 target 且不是 dry-run fallback 的执行器。
- fallback：`DryRunDeploymentExecutor` 支持所有 target。
- unsupported：如果没有任何执行器支持 target，则返回明确错误。

显式启用边界：

| 执行器 | 配置项 | 默认 |
| --- | --- | --- |
| Docker | `zerocode.deploy.executors.docker.enabled` / `DEPLOY_DOCKER_EXECUTOR_ENABLED` | `false` |
| GitHub Actions | `zerocode.deploy.executors.github-actions.enabled` / `DEPLOY_GITHUB_ACTIONS_EXECUTOR_ENABLED` | `false` |
| Kubernetes | `zerocode.deploy.executors.kubernetes.enabled` / `DEPLOY_KUBERNETES_EXECUTOR_ENABLED` | `false` |

默认服务端口：

- `deploy-service`: `8081`
- `platform-service`: `8080`，context path 为 `/api`

当前 Docker executor 的执行边界：

- `zerocode.deploy.executors.docker.enabled=false` 时不参与路由，继续使用 dry-run fallback。
- `zerocode.deploy.executors.docker.enabled=true` 但 `execution-mode` 不是 `real` 时，返回 `skipped`，不执行真实命令。
- 只有同时满足 `enabled=true` 和 `execution-mode=real` 时，才会进入真实 Docker 路径。
- 真实 Docker 路径负责下载 artifact ZIP、进行 zip-slip 安全解压、执行 `docker build`，并在配置启用时执行 `docker push`。
- Docker 命令通过 `DockerCommandRunner` 抽象执行，便于测试中替换为 fake runner。
- 真实执行失败会返回 `failed`，成功会返回 `succeeded`。

Docker executor 配置：

| 配置项 | 环境变量 | 默认 | 说明 |
| --- | --- | --- | --- |
| `zerocode.deploy.executors.docker.enabled` | `DEPLOY_DOCKER_EXECUTOR_ENABLED` | `false` | 是否注册 Docker target executor |
| `zerocode.deploy.executors.docker.execution-mode` | `DEPLOY_DOCKER_EXECUTION_MODE` | `dry-run` | 只有 `real` 才执行真实命令 |
| `zerocode.deploy.executors.docker.workspace-root` | `DEPLOY_DOCKER_WORKSPACE_ROOT` | `/tmp/zerocode-docker-deployments` | artifact 临时解压根目录 |
| `zerocode.deploy.executors.docker.command-timeout-seconds` | `DEPLOY_DOCKER_COMMAND_TIMEOUT_SECONDS` | `300` | 单条 Docker 命令超时 |
| `zerocode.deploy.executors.docker.image-repository-prefix` | `DEPLOY_DOCKER_IMAGE_REPOSITORY_PREFIX` | `zerocode` | 镜像名前缀 |
| `zerocode.deploy.executors.docker.push-enabled` | `DEPLOY_DOCKER_PUSH_ENABLED` | `false` | 是否执行 `docker push` |

当前 GitHub Actions executor 的执行边界：

- `zerocode.deploy.executors.github-actions.enabled=false` 时不参与路由，继续使用 dry-run fallback。
- `zerocode.deploy.executors.github-actions.enabled=true` 但 `execution-mode` 不是 `real` 时，返回 `skipped`，不调用 GitHub API。
- 只有同时满足 `enabled=true`、`execution-mode=real` 且 token/owner/repo/workflow/ref 配置完整时，才会调用 GitHub workflow dispatch API。
- dispatch 输入包含 `app_id`、`version_no`、`project_type`、`artifact_url`，由 GitHub Actions workflow 负责后续构建、推送和发布。
- GitHub API 调用通过 `GithubActionsClient` 抽象执行，便于测试中替换为 fake client。
- GitHub 返回 `204 No Content` 视为 `succeeded`；配置不完整视为 `skipped`；API 调用失败视为 `failed`。

GitHub Actions executor 配置：

| 配置项 | 环境变量 | 默认 | 说明 |
| --- | --- | --- | --- |
| `zerocode.deploy.executors.github-actions.enabled` | `DEPLOY_GITHUB_ACTIONS_EXECUTOR_ENABLED` | `false` | 是否注册 GitHub Actions target executor |
| `zerocode.deploy.executors.github-actions.execution-mode` | `DEPLOY_GITHUB_ACTIONS_EXECUTION_MODE` | `dry-run` | 只有 `real` 才调用 GitHub API |
| `zerocode.deploy.executors.github-actions.api-base-url` | `DEPLOY_GITHUB_ACTIONS_API_BASE_URL` | `https://api.github.com` | GitHub API base URL |
| `zerocode.deploy.executors.github-actions.token` | `DEPLOY_GITHUB_ACTIONS_TOKEN` | 空 | GitHub token，不允许写入日志 |
| `zerocode.deploy.executors.github-actions.owner` | `DEPLOY_GITHUB_ACTIONS_OWNER` | 空 | 仓库 owner |
| `zerocode.deploy.executors.github-actions.repo` | `DEPLOY_GITHUB_ACTIONS_REPO` | 空 | 仓库名 |
| `zerocode.deploy.executors.github-actions.workflow-id` | `DEPLOY_GITHUB_ACTIONS_WORKFLOW_ID` | 空 | workflow 文件名或 id |
| `zerocode.deploy.executors.github-actions.ref` | `DEPLOY_GITHUB_ACTIONS_REF` | `main` | workflow dispatch ref |

当前 Kubernetes executor 的执行边界：

- `zerocode.deploy.executors.kubernetes.enabled=false` 时不参与路由，继续使用 dry-run fallback。
- `zerocode.deploy.executors.kubernetes.enabled=true` 但 `execution-mode` 不是 `real` 时，返回 `skipped`，不调用 `kubectl`。
- 只有同时满足 `enabled=true` 和 `execution-mode=real` 时，才会生成 Kubernetes manifest 并执行 `kubectl apply`。
- 当前 manifest 由 deploy-service 生成，包含 Deployment 和 Service。
- 镜像名按 `image-repository-prefix/app-{appId}:v{versionNo}` 生成，应与 Docker/GitHub Actions 构建推送出的镜像保持一致。
- `kubeconfig` 可选；配置后通过 `KUBECONFIG` 环境变量传给 `kubectl`，不会写入日志。
- Kubernetes 命令通过 `KubernetesCommandRunner` 抽象执行，便于测试中替换为 fake runner。
- kubectl exit code `0` 视为 `succeeded`；非 0 或异常视为 `failed`。

Kubernetes executor 配置：

| 配置项 | 环境变量 | 默认 | 说明 |
| --- | --- | --- | --- |
| `zerocode.deploy.executors.kubernetes.enabled` | `DEPLOY_KUBERNETES_EXECUTOR_ENABLED` | `false` | 是否注册 Kubernetes target executor |
| `zerocode.deploy.executors.kubernetes.execution-mode` | `DEPLOY_KUBERNETES_EXECUTION_MODE` | `dry-run` | 只有 `real` 才执行 kubectl |
| `zerocode.deploy.executors.kubernetes.namespace` | `DEPLOY_KUBERNETES_NAMESPACE` | `default` | 部署命名空间 |
| `zerocode.deploy.executors.kubernetes.kubectl-binary` | `DEPLOY_KUBERNETES_KUBECTL_BINARY` | `kubectl` | kubectl 可执行文件 |
| `zerocode.deploy.executors.kubernetes.kubeconfig` | `DEPLOY_KUBERNETES_KUBECONFIG` | 空 | 可选 kubeconfig 路径 |
| `zerocode.deploy.executors.kubernetes.command-timeout-seconds` | `DEPLOY_KUBERNETES_COMMAND_TIMEOUT_SECONDS` | `300` | kubectl 命令超时 |
| `zerocode.deploy.executors.kubernetes.image-repository-prefix` | `DEPLOY_KUBERNETES_IMAGE_REPOSITORY_PREFIX` | `zerocode` | 镜像名前缀 |
| `zerocode.deploy.executors.kubernetes.service-port` | `DEPLOY_KUBERNETES_SERVICE_PORT` | `80` | Service 端口 |

部署状态：

| 状态 | 语义 |
| --- | --- |
| `planned` | 已创建部署计划，未执行真实部署 |
| `running` | 真实执行器已开始执行 |
| `succeeded` | 真实执行器完成部署 |
| `failed` | 真实执行器执行失败 |
| `skipped` | 环境或配置不可用，跳过执行 |

当前 dry-run executor 只会返回 `planned`。Docker/GitHub Actions/Kubernetes 真实执行器已实现受控 real mode，并会按执行结果返回 `succeeded`、`failed` 或 `skipped`。`running` 已作为状态契约保留，后续异步部署或日志流式化时使用。

当前 API：

```http
POST /deployments
Content-Type: application/json

{
  "appId": 10,
  "versionNo": 2,
  "projectType": "vue",
  "artifactUrl": "https://example.com/app.zip",
  "target": "docker"
}
```

响应核心字段：

```json
{
  "code": 200,
  "message": "ok",
  "data": {
    "status": "planned",
    "plannedCommands": [],
    "executionLogs": []
  }
}
```

查询：

```http
GET /deployments/{id}
```

后续生产化 `deploy-service` 应继续增强：

- 凭据管理和权限隔离。
- 镜像仓库、tag、访问 URL 的明确契约。
- 异步执行、状态轮询和日志流。
- 超时、重试和取消部署。
- DB 持久化替换本地 JSON 文件。
- 真实环境中的 Docker/GitHub Actions/Kubernetes 集成验收。

后续不应由 `platform-service` 直接执行用户项目构建或生产部署命令。
