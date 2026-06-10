# Deployment

## 1. 当前目标

Phase 3 的部署链路先实现最小可验证能力：

- ZIP 导出时附加可直接 Docker 部署的文件。
- 支持 HTML 静态项目。
- 支持 Vue/React Vite 项目。
- 不在平台服务内直接执行用户项目构建或部署命令。

当前已新增 `deploy-service` 最小骨架和 executor routing 边界。Docker、GitHub Actions、Kubernetes 自动部署执行器默认不启用；显式启用后当前仍只返回 skipped，不执行真实命令。

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

## 7. 后续 deploy-service 边界

当前 `deploy-service` 最小骨架负责：

- 接收部署请求。
- 校验 appId、versionNo、projectType、artifactUrl、target。
- 创建部署记录。
- 将部署记录持久化到本地 JSON 文件。
- 返回 `planned` 状态。
- 返回建议命令。
- 通过 dry-run executor 生成执行日志。
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
- 实现：`DryRunDeploymentExecutor`
- 状态：只返回 `planned`
- 日志：记录 target、artifact、planned command
- 安全边界：不执行真实 Docker、GitHub Actions、Kubernetes 命令

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

当前显式启用后的 Docker/GitHub Actions/Kubernetes executor 仍为安全占位实现：

- 返回部署状态 `skipped`。
- 写入 executionLogs，说明当前构建未实现真实命令执行。
- 不调用宿主机 Docker、GitHub API、kubectl 或 Kubernetes API。

部署状态：

| 状态 | 语义 |
| --- | --- |
| `planned` | 已创建部署计划，未执行真实部署 |
| `running` | 真实执行器已开始执行 |
| `succeeded` | 真实执行器完成部署 |
| `failed` | 真实执行器执行失败 |
| `skipped` | 环境或配置不可用，跳过执行 |

当前 dry-run executor 只会返回 `planned`。后续真实 Docker/GitHub Actions/Kubernetes executor 必须显式写入 `running`、`succeeded`、`failed` 或 `skipped`。

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

后续完整 `deploy-service` 应负责：

- 接收已保存版本。
- 拉取或生成部署包。
- 在隔离环境中构建镜像。
- 推送镜像到镜像仓库。
- 创建部署记录。
- 对接 GitHub Actions 或 Kubernetes。
- 返回部署状态、日志和访问 URL。

后续不应由 `platform-service` 直接执行用户项目构建或生产部署命令。
