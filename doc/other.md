# 推荐微服务拆分

---

## Java 服务

| 服务              | 职责     |
| --------------- | ------ |
| gateway-service | API 网关 |
| user-service    | 用户系统   |
| project-service | 项目管理   |
| chat-service    | AI 对话  |
| sandbox-service | 沙箱调度   |
| file-service    | 文件系统   |
| deploy-service  | 部署系统   |

---

## Python 服务

| 服务              | 职责     |
| --------------- | ------ |
| ai-orchestrator | AI 工作流 |
| code-generator  | 代码生成   |
| code-fixer      | 自动修复   |
| ui-designer     | UI 生成  |
| test-agent      | 自动测试   |

---

# AI Agent 架构（核心）

---

# 推荐 Agent 工作流

```text id="g6o0dz"
User Request
    ↓
Planner Agent
    ↓
UI Designer Agent
    ↓
Code Generator Agent
    ↓
Code Fixer Agent
    ↓
Sandbox Runner
    ↓
Test Agent
    ↓
Result
```

---

# Agent 职责

| Agent          | 作用       |
| -------------- | -------- |
| Planner        | 拆解需求     |
| UI Designer    | 生成 UI 方案 |
| Code Generator | 生成代码     |
| Fixer          | 修复错误     |
| Tester         | 自动测试     |

---

# 开发阶段规划（非常重要）

---

# Phase 1（MVP）

周期：

```text id="e1h6d7"
4~6 周
```

功能：

* AI HTML 生成
* iframe 预览
* 项目保存
* 对话修改

---

# Phase 2

周期：

```text id="go7l3p"
6~8 周
```

功能：

* React/Vue 项目
* 多文件生成
* ZIP 导出
* AST 修复

---

# Phase 3

周期：

```text id="xjlwmg"
8~12 周
```

功能：

* 多 Agent
* Playwright 自动测试
* Docker 沙箱
* 自动部署

---

# 部署架构（生产级）

---

# 推荐架构

```text id="3wqzj0"
Nginx
  ↓
Gateway
  ↓
Java Services
  ↓
RabbitMQ
  ↓
Python AI Cluster
```

---

# 推荐部署技术

| 模块    | 技术                   |
| ----- | -------------------- |
| 容器    | Docker               |
| 编排    | Kubernetes           |
| 网关    | Nginx                |
| CI/CD | GitHub Actions       |
| 监控    | Prometheus + Grafana |

---

# 最终推荐技术栈

---

## 前端

```text id="h4hl8s"
Vue3
TypeScript
Vite
TailwindCSS
Monaco
GrapesJS
```

---

## Java 平台层

```text id="4h4ws6"
Spring Boot 3
MyBatis Plus
Redis
RabbitMQ
MySQL
MinIO
Sa-Token
```

---

## Python AI 层

```text id="8ny30e"
FastAPI
LangGraph
PydanticAI
Playwright
CrewAI
```

---

## AI 模型

Deepseek-v4-pro

---
