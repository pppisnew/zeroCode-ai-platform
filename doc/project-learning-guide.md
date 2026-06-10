# ZeroCode AI Platform 新人学习指南

本文档面向完全没有经验的新手开发者。目标不是简单介绍项目，而是帮助你像跟着高级工程师读代码一样，逐步理解整个项目，直到能够自己独立阅读和分析代码。

阅读原则：

- 先理解业务，再理解技术。
- 先看整体，再看局部。
- 第一次看到术语时，先理解它解决什么问题。
- 不要一上来钻进某个文件，要先知道它在系统里扮演什么角色。

# 1. 用一句话讲清项目

ZeroCode AI Platform 是一个“你用文字描述想要的网站，系统自动生成、预览、编辑、保存、导出并尝试部署这个网站”的 AI 应用生成平台。

类比：

```text
用户说需求
-> AI 网站工厂生成代码
-> 在线工作台预览和编辑
-> 平台保存版本
-> 导出 ZIP
-> 创建部署任务
```

# 2. 项目解决了什么问题

## 2.1 用户痛点

普通用户或初级开发者想做一个 Web 应用，通常会遇到这些问题：

- 不知道怎么写 HTML、CSS、JavaScript。
- 不知道 Vue、React 项目该有哪些文件。
- 不知道代码能不能运行。
- 不知道怎么保存历史版本。
- 不知道怎么打包部署。
- AI 生成代码后，可能有危险代码或坏代码，需要检查。

## 2.2 业务目标

这个项目要完成一条完整的应用生成链路：

```text
用户输入需求
-> AI 生成项目代码
-> 在线预览
-> 可视化/代码编辑
-> 保存版本
-> 历史恢复
-> ZIP 导出
-> 创建部署任务
```

## 2.3 核心价值

它的核心价值不是“生成一段代码”，而是生成一个可管理的项目：

- 生成的是多文件项目，不是零散代码片段。
- 项目可以保存、恢复、导出。
- 有三层安全校验，避免危险代码进入系统。
- 有部署服务，为后续自动发布打基础。
- 有测试和文档，后续可以继续扩展。

# 3. 项目整体运行流程

本节从“用户打开系统”开始，把一次生成过程讲成一个故事。

## 3.1 用户打开前端页面

用户访问：

```text
http://localhost:5173
```

进入的是 `frontend` 里的 Vue 工作台，核心页面是：

```text
frontend/src/pages/WorkspacePage.vue
```

这个页面像一个开发工作台，里面有：

- 输入框：写需求。
- 项目类型选择：HTML / Vue / React。
- 文件编辑器：看和改代码。
- 预览窗口：看生成效果。
- 版本列表：切换历史版本。
- 部署面板：选择 Docker / GitHub Actions / Kubernetes。

## 3.2 用户输入需求并点击生成

例如用户输入：

```text
生成一个 Todo 应用
```

前端会进入：

```text
frontend/src/hooks/useWorkspaceActions.ts
```

核心函数是：

```text
handleGenerate()
```

它会调用 API：

```text
POST /api/generations/html
```

## 3.3 Java 平台服务接收请求

请求先进入 Java 后端 `platform-service`：

```text
backend/platform-service/src/main/java/com/zerocode/platform/controller/GenerationController.java
```

术语解释：

- Controller：控制器，可以理解为“接待员”。
- 它负责接收 HTTP 请求，但不应该自己做复杂业务。

Controller 收到请求后，会交给：

```text
AiGenerationServiceImpl
```

术语解释：

- Service：服务层，可以理解为“真正处理业务的人”。
- 它负责组织业务流程，比如调用 AI、保存应用、创建版本。

## 3.4 Java 调用 Python AI 服务

`AiGenerationServiceImpl` 通过 HTTP 调用 Python 服务：

```text
POST http://localhost:8000/generations/html
```

为什么 Java 不自己生成代码？

因为项目做了职责拆分：

- Java 负责业务系统：应用、版本、保存、导出、部署入口。
- Python 负责 AI 生成、修复、测试。
- 前端负责用户交互。

这叫解耦。解耦就是减少模块之间的强依赖，让每个模块专心做自己的事情。

## 3.5 Python AI 服务执行生成工作流

Python 服务入口在：

```text
ai-services/ai-orchestrator/app/routers/generation_router.py
```

真正的生成流程在：

```text
ai-services/ai-orchestrator/app/workflows/html_generation_workflow.py
```

流程是：

```text
planner -> ui -> code -> fix -> test
```

类比成工厂流水线：

```text
需求分析员 -> UI 设计员 -> 代码生成员 -> 修复员 -> 测试员
```

对应代码中的节点：

- `planner_node`
- `ui_node`
- `code_node`
- `fix_node`
- `test_node`

术语解释：

- Agent：在这个项目里，可以理解为“负责某个具体任务的 AI 工人”。
- Workflow：工作流，可以理解为“这些工人的工作顺序”。

## 3.6 Python 返回生成项目

Python 返回的不是一段普通字符串，而是结构化项目：

```json
{
  "projectName": "zerocode-html-app",
  "projectType": "html",
  "files": [
    {
      "filePath": "index.html",
      "fileType": "html",
      "content": "..."
    }
  ]
}
```

项目真正关心的是文件列表：

```text
files = 多个 GeneratedFile
```

## 3.7 Java 保存应用和版本

Java 收到 Python 返回结果后：

1. 如果是新项目，创建 `app`。
2. 创建 `app_version`。
3. 把生成项目 JSON 存到数据库字段 `ai_response`。
4. 返回给前端。

核心文件：

```text
backend/platform-service/src/main/java/com/zerocode/platform/service/impl/AiGenerationServiceImpl.java
backend/platform-service/src/main/java/com/zerocode/platform/service/impl/AppVersionServiceImpl.java
```

## 3.8 前端展示项目

前端拿到结果后：

- 更新当前 app id。
- 更新当前 version no。
- 把 files 放进 Pinia store。
- Monaco Editor 显示代码。
- iframe 预览 HTML。
- 如果是 HTML 项目，还可以用 GrapesJS 可视化编辑。

## 3.9 用户保存版本

用户修改代码后点击保存。

前端先做安全检查：

```text
frontend/src/utils/projectFileSecurity.ts
```

检查通过后调用：

```text
POST /api/apps/{id}/versions
```

Java 后端还会再检查一次：

```text
ProjectFileValidator.validateProject()
```

为什么前后端都检查？

因为前端检查只是用户体验好，真正的安全底线必须在后端。

## 3.10 用户导出 ZIP

用户点击导出 ZIP：

```text
GET /api/apps/{id}/versions/{versionNo}/zip
```

Java 会：

1. 读取版本。
2. 再次校验文件安全。
3. 写入用户项目文件。
4. 额外写入部署文件：
   - `Dockerfile`
   - `nginx.conf`
   - `DEPLOYMENT.md`

核心文件：

```text
backend/platform-service/src/main/java/com/zerocode/platform/util/DeploymentPackageBuilder.java
```

## 3.11 用户创建部署任务

用户点击部署时，前端调用：

```text
POST /api/apps/{id}/versions/{versionNo}/deployments
```

platform-service 再调用 deploy-service：

```text
POST http://localhost:8081/deployments
```

deploy-service 根据 target 选择执行器：

```text
docker
github-actions
kubernetes
```

默认不会真的部署，只会 dry-run，避免误操作生产环境。

# 4. 系统架构设计

## 4.1 为什么要分层

分层就是“不同的人做不同的事”。

类比餐厅：

- 服务员：接收顾客点单。
- 厨师：做菜。
- 仓库：管理食材。
- 收银：处理账单。

软件里也是一样：

```text
前端
-> Java 平台服务
-> Python AI 服务
-> 数据库 / 部署服务
```

如果所有逻辑都写在一个地方，会出现问题：

- 难改。
- 难测。
- 难排错。
- 一个模块坏了影响全部。

## 4.2 当前项目怎么拆

整体结构：

```text
frontend
  |
  | HTTP
  v
backend/platform-service
  |
  | HTTP
  v
ai-services/ai-orchestrator

backend/platform-service
  |
  | DB
  v
MySQL

backend/platform-service
  |
  | HTTP
  v
backend/deploy-service
```

## 4.3 每层职责

### 前端 frontend

负责：

- 展示页面。
- 接收用户输入。
- 调 API。
- 展示文件、预览、版本、部署结果。
- 做第一层安全检查。

前端不应该直接操作数据库。

### platform-service

负责：

- 应用管理。
- 版本管理。
- 调用 AI 服务。
- 保存生成结果。
- 导出 ZIP。
- 调用 deploy-service。

它是业务中心。

### ai-orchestrator

负责：

- 根据 prompt 生成代码。
- 修复项目结构。
- 做 sandbox 检查。
- 保证返回结构化项目。

它是 AI 工厂。

### deploy-service

负责：

- 接收部署请求。
- 记录部署任务。
- 根据 target 选择部署执行器。
- 默认 dry-run。
- 真实执行必须显式开启。

它是部署控制台。

### infra

负责本地基础设施：

- MySQL
- Redis
- RabbitMQ
- MinIO

目前 MySQL 是最核心的，其他更多是后续扩展基础。

## 4.4 解耦思想

解耦就是减少互相依赖。

本项目中的体现：

- 前端不直接调 Python，而是调 Java。
- Java 不直接执行 Docker，而是交给 deploy-service。
- deploy-service 不写死一种部署方式，而是用 executor。

这样以后要换 AI 服务、换部署方式、换存储，都不用推翻整个系统。

## 4.5 模块化思想

模块化就是把大问题拆成小问题。

例如 AI 生成不是一个大函数，而是拆成：

```text
planner
ui
code
fix
test
```

好处：

- 每一步更清楚。
- 单独测试更容易。
- 后续替换某一步更容易。

# 5. 技术栈解析

## 5.1 Vue 3

Vue 是前端框架，负责把数据变成页面。

为什么需要它？

因为工作台页面里有很多动态数据：

- 当前项目。
- 当前文件。
- 当前版本。
- 生成状态。
- 部署状态。

为什么选择它？

- 上手相对简单。
- 适合做工作台类页面。
- 和 TypeScript、Vite 配合成熟。

项目中负责：

```text
frontend/src/pages
frontend/src/components
```

## 5.2 TypeScript

TypeScript 是带类型的 JavaScript。

类型可以理解成“提前告诉代码这个数据应该长什么样”。

例如：

```text
GeneratedProject
GeneratedFile
Deployment
```

为什么需要？

因为前后端传递的数据结构复杂，没有类型很容易传错字段。

## 5.3 Vite

Vite 是前端开发和构建工具。

它负责：

- 本地启动 dev server。
- 打包生产文件。
- 处理 Vue、CSS、TypeScript。

配置文件：

```text
frontend/vite.config.ts
```

## 5.4 Pinia

Pinia 是 Vue 状态管理工具。

状态可以理解成“多个组件共享的数据”。

项目中它保存：

- 当前 app id。
- 当前版本号。
- 当前文件列表。
- 当前项目类型。

如果不用状态管理，数据会在组件之间传来传去，很乱。

## 5.5 Arco Design

Arco 是 UI 组件库。

它提供按钮、消息、表单、布局等常用组件。

项目中常见用途：

- `Message.success()`
- `Message.error()`
- 页面控件。

## 5.6 Monaco Editor

Monaco 是 VS Code 同款代码编辑器核心。

项目中负责代码编辑体验：

- 语法高亮。
- 编辑器布局。
- 多语言显示。

为什么不用普通 textarea？

因为这是代码生成平台，用户需要真正编辑代码。

## 5.7 GrapesJS

GrapesJS 是可视化网页编辑器。

它让用户不用直接写 HTML，也能编辑页面结构。

项目中只给 HTML 项目使用，因为 Vue/React 项目结构更复杂，暂时不进入 Visual 模式。

## 5.8 Java 21 + Spring Boot

Java 是后端语言。Spring Boot 是 Java Web 后端框架。

它负责：

- 接 HTTP 请求。
- 调用服务。
- 连接数据库。
- 返回 JSON。

为什么选择它？

- 企业开发常用。
- 结构清晰。
- 生态成熟。

## 5.9 MyBatis-Plus

MyBatis-Plus 是数据库访问工具。

你可以把它理解成“Java 对数据库说话的翻译器”。

项目里 Mapper 负责把 Java 对象存进 MySQL。

## 5.10 MySQL

MySQL 是关系型数据库。

关系型数据库适合存结构化业务数据：

- app
- app_version
- user
- chat_message
- ai_task

## 5.11 Python 3.12 + FastAPI

Python 适合 AI 编排。FastAPI 是 Python Web 框架。

为什么 AI 层用 Python？

因为 AI、LangGraph、Playwright、脚本工具生态在 Python 里更顺手。

## 5.12 LangGraph

LangGraph 是工作流编排工具。

项目用它把 AI 生成拆成：

```text
planner -> ui -> code -> fix -> test
```

这样比“一次性让 AI 生成所有东西”更可控。

## 5.13 Pydantic

Pydantic 是 Python 数据校验工具。

它负责保证请求和返回数据结构正确。

例如：

- 文件路径不能太长。
- 文件数量不能太多。
- 项目类型必须合法。

## 5.14 Docker

Docker 是容器工具。

容器可以理解成“隔离的小房间”。

项目用 Docker 做两件事：

- 生成项目的构建检查。
- 部署执行器的真实 Docker 路径。

为什么需要隔离？

因为 AI 生成的代码不能随便在宿主机运行。

## 5.15 Playwright

Playwright 是浏览器自动化工具。

项目中用于可选 browser sandbox：

- 打开页面。
- 检查有没有文本。
- 检查有没有可见元素。
- 截图。

# 6. 项目目录结构详解

## 6.1 顶层目录

```text
zeroCode-ai-platform
├── frontend
├── backend
│   ├── platform-service
│   └── deploy-service
├── ai-services
│   └── ai-orchestrator
├── infra
└── doc
```

## 6.2 frontend

前端工作台。

重点文件：

```text
frontend/src/pages/WorkspacePage.vue
frontend/src/hooks/useWorkspaceActions.ts
frontend/src/api/generations.ts
frontend/src/stores/workspace.ts
frontend/src/utils/projectFileSecurity.ts
frontend/src/utils/previewDocument.ts
```

推荐阅读顺序：

```text
WorkspacePage.vue
-> useWorkspaceActions.ts
-> api/generations.ts
-> projectFileSecurity.ts
-> previewDocument.ts
```

## 6.3 backend/platform-service

Java 平台核心服务。

重点文件：

```text
GenerationController.java
AppController.java
AiGenerationServiceImpl.java
AppVersionServiceImpl.java
ProjectFileValidator.java
DeploymentPackageBuilder.java
```

推荐阅读顺序：

```text
Controller
-> Service
-> Mapper
-> Entity
-> Validator
```

## 6.4 backend/deploy-service

部署服务。

重点文件：

```text
DeploymentController.java
InMemoryDeploymentService.java
DeploymentExecutorRouter.java
DockerDeploymentExecutor.java
GithubActionsDeploymentExecutor.java
KubernetesDeploymentExecutor.java
```

推荐阅读顺序：

```text
Controller
-> Service
-> Router
-> Executor
-> Repository
```

## 6.5 ai-services/ai-orchestrator

Python AI 服务。

重点文件：

```text
generation_router.py
html_generation_service.py
html_generation_workflow.py
html_generation_agents.py
html_sandbox.py
docker_sandbox.py
project_repair.py
```

推荐阅读顺序：

```text
router
-> service
-> workflow
-> agents
-> tools
-> models
```

## 6.6 infra

本地基础设施。

重点文件：

```text
infra/docker-compose.yml
infra/mysql/init.sql
```

## 6.7 doc

项目文档。

最重要：

```text
doc/operations.md
doc/task-current.md
doc/deployment.md
doc/security-rules.md
doc/project-learning-guide.md
```

# 7. 核心业务模块详解

## 7.1 前端 Workspace 模块

职责：

- 展示工作台。
- 触发生成、保存、导出、部署。
- 管理当前文件列表。
- 展示代码和预览。

核心文件：

```text
WorkspacePage.vue
useWorkspaceActions.ts
workspace.ts
```

调用链：

```text
用户点击生成
-> handleGenerate()
-> generateHtml()
-> Java /api/generations/html
-> 返回项目
-> workspaceStore.setFiles()
-> 编辑器和预览更新
```

为什么这样设计？

页面组件主要负责展示，业务动作集中在 hook 里，这样组件不会越来越臃肿。

## 7.2 AI 生成模块

职责：

- 接收 prompt。
- 根据项目类型生成文件。
- 修复缺失结构。
- 做 sandbox 检查。
- 返回 GeneratedProject。

核心文件：

```text
html_generation_workflow.py
html_generation_agents.py
project_repair.py
html_sandbox.py
docker_sandbox.py
```

流程：

```text
prompt
-> planner_node
-> ui_node
-> code_node
-> fix_node
-> test_node
-> GeneratedProject
```

为什么拆成多个节点？

因为 AI 生成不是简单函数调用。拆开后每一步更容易测试、修复和替换。

## 7.3 应用与版本模块

职责：

- 保存 app。
- 保存 app_version。
- 查询版本。
- 恢复历史。
- 导出 ZIP。

核心文件：

```text
AppController.java
AppVersionServiceImpl.java
AppVersionMapper.java
AppVersionEntity.java
```

输入示例：

```json
{
  "prompt": "生成 Todo 应用",
  "project": {
    "projectName": "zerocode-html-app",
    "files": []
  }
}
```

输出示例：

```json
{
  "appId": 1,
  "versionNo": 2,
  "project": {}
}
```

为什么要有版本？

因为 AI 生成和用户修改都是反复迭代的。版本让用户可以回到过去某一次结果。

## 7.4 安全校验模块

职责：

防止危险代码进入预览、保存、导出。

三层：

```text
前端 projectFileSecurity.ts
Python html_sandbox.py
Java ProjectFileValidator.java
```

主要规则：

- 禁止内联 script。
- 禁止 onclick 等内联事件。
- 禁止外部 URL。
- 禁止 fetch/WebSocket。
- 禁止 eval/new Function。
- 禁止路径穿越 `../`。

为什么三层都要做？

因为任何一层都可能被绕过。

类比：

```text
小区门口有保安
楼栋门口还有门禁
家门口还有锁
```

## 7.5 ZIP 导出模块

职责：

- 把项目文件打包。
- 附加部署文件。
- 防止 zip-slip 路径攻击。

核心文件：

```text
DeploymentPackageBuilder.java
AppController.createZip()
```

术语解释：

zip-slip 是一种压缩包路径攻击。例如 ZIP 里有人放：

```text
../../../../etc/passwd
```

如果不检查，解压时可能覆盖系统文件。所以项目用 `safeProjectPath()` 检查路径。

## 7.6 部署模块

职责：

- 创建部署记录。
- 选择部署目标。
- 默认 dry-run。
- 可配置真实执行。

核心文件：

```text
DeploymentController.java
InMemoryDeploymentService.java
DeploymentExecutorRouter.java
DeploymentExecutor.java
DockerDeploymentExecutor.java
GithubActionsDeploymentExecutor.java
KubernetesDeploymentExecutor.java
```

调用链：

```text
前端点击部署
-> platform-service 创建部署请求
-> deploy-service
-> DeploymentExecutorRouter
-> 选择 Docker/GitHub/Kubernetes executor
-> 保存 DeploymentRecord
-> 返回 DeploymentVO
```

# 8. 数据流转分析

## 8.1 一个生成请求如何进入系统

流程：

```text
浏览器
  |
  | POST /api/generations/html
  v
GenerationController
  |
  v
AiGenerationServiceImpl
  |
  | POST /generations/html
  v
Python AI Orchestrator
  |
  v
LangGraph workflow
  |
  v
GeneratedProject
  |
  v
Java 保存 App + AppVersion
  |
  v
MySQL
  |
  v
返回前端
```

## 8.2 Controller 是什么

Controller 是 HTTP 入口。

它负责：

- 接收请求。
- 解析参数。
- 调用 Service。
- 返回响应。

它不应该写大量业务逻辑。

## 8.3 Service 是什么

Service 是业务处理层。

它负责：

- 判断业务规则。
- 调用其他服务。
- 调用 Mapper/Repository。
- 组织返回结果。

例如 `AiGenerationServiceImpl` 负责调用 Python、创建 app、保存版本。

## 8.4 Repository / Mapper 是什么

Repository / Mapper 是数据访问层。

Java 平台服务里：

```text
AppVersionMapper
```

负责和 MySQL 的 `app_version` 表交互。

deploy-service 里：

```text
DeploymentRepository
FileDeploymentRepository
```

负责保存部署记录。当前是 JSON 文件，后续可以换数据库。

## 8.5 数据库是什么角色

数据库是系统的“长期记忆”。

前端刷新页面后，内存会丢。数据库保存 app 和 version，系统才能恢复历史。

# 9. 项目中的设计思想

## 9.1 分层架构

分层就是：

```text
Controller 接请求
Service 做业务
Mapper/Repository 存数据
Util 做工具逻辑
```

如果不分层：

- Controller 会变得巨大。
- 测试困难。
- 改数据库会影响接口。
- 改业务会影响页面。

## 9.2 策略模式

策略模式就是“同一个动作，有多种做法”。

项目里最明显的是部署 executor：

```text
DeploymentExecutor
├── DryRunDeploymentExecutor
├── DockerDeploymentExecutor
├── GithubActionsDeploymentExecutor
└── KubernetesDeploymentExecutor
```

部署目标不同，策略不同。

如果不用策略模式，就会写一大堆：

```text
if target == docker
else if target == github
else if target == kubernetes
```

后续加新部署方式会越来越乱。

## 9.3 工厂思想

`DeploymentPackageBuilder` 根据项目类型生成部署文件。

HTML、Vue、React 的 Dockerfile 不一样。它相当于一个“部署文件制造器”。

如果没有这个集中构建逻辑，导出 ZIP 的地方会塞满模板字符串。

## 9.4 责任链 / 流水线思想

Python AI workflow 是典型流水线：

```text
planner -> ui -> code -> fix -> test
```

每一步只做自己的事。如果一步失败或需要增强，可以单独改。

## 9.5 模板方法思想

项目里有很多固定结构：

- Vue 项目骨架。
- React 项目骨架。
- Dockerfile 模板。
- Kubernetes manifest 模板。

这些都体现“固定流程 + 可变内容”。

## 9.6 配置化思想

很多能力不是写死的，而是环境变量控制：

```text
DEPLOY_DOCKER_EXECUTOR_ENABLED
DEPLOY_DOCKER_EXECUTION_MODE
ZEROCODE_ENABLE_DOCKER_SANDBOX
```

为什么？

因为开发环境、测试环境、生产环境不一样。

配置化可以做到：

- 默认安全。
- 需要时开启。
- 不改代码切换行为。

## 9.7 可扩展性

这个项目以后可以扩展：

- 新增 Svelte 项目类型。
- 新增 Vercel 部署 executor。
- 新增数据库型 DeploymentRepository。
- 新增更强 AST 安全分析。
- 新增真实 AI 模型调用。

# 10. 项目难点分析

## 10.1 真正困难的地方

第一，难点不是生成代码，而是生成可管理的项目。

AI 生成一段 HTML 很简单。难的是：

- 文件结构正确。
- Vue/React 入口正确。
- 能预览。
- 能保存。
- 能导出。
- 能部署。
- 能通过安全校验。

第二，安全很难。

AI 可能生成：

```html
<script>alert(1)</script>
```

或者：

```js
fetch("https://evil.example")
```

系统必须防住。

第三，多服务协作难。

这里有：

- 前端。
- Java platform-service。
- Python ai-orchestrator。
- Java deploy-service。
- MySQL。
- Docker。

新手容易不知道请求去哪了。

## 10.2 新手最容易懵的地方

### 为什么有多个后端

因为职责不同：

- platform-service 管业务。
- ai-orchestrator 管 AI。
- deploy-service 管部署。

### 为什么保存前检查，导出前还检查

因为数据可能来自不同入口。历史版本也可能已经存在危险内容。所以导出前必须再检查。

### 为什么部署默认不真实执行

因为真实部署有风险：

- 可能构建恶意代码。
- 可能推送错误镜像。
- 可能覆盖 Kubernetes 服务。
- 可能泄露 token。

所以默认 dry-run 是企业开发里的安全设计。

### 为什么测试这么多

企业开发不是“能跑就行”。要保证以后改代码不会悄悄弄坏旧功能。

# 11. 新手学习路线

## 第一步：先理解业务，不看代码

先把这个流程背下来：

```text
输入需求
-> 生成项目
-> 预览编辑
-> 保存版本
-> 导出 ZIP
-> 创建部署
```

目标：知道系统是干什么的。

## 第二步：读 README 和 operations

先看：

```text
README.md
doc/operations.md
```

目标：知道怎么启动项目，服务有哪些。

## 第三步：看前端主流程

阅读顺序：

```text
frontend/src/pages/WorkspacePage.vue
frontend/src/hooks/useWorkspaceActions.ts
frontend/src/api/generations.ts
frontend/src/stores/workspace.ts
```

你要搞懂：

- 用户点击按钮后调用哪个函数。
- 函数调哪个 API。
- 返回数据放到哪里。

## 第四步：看 Java Controller

阅读：

```text
GenerationController.java
AppController.java
```

目标：知道 API 入口。

重点关注注解：

```java
@PostMapping
@GetMapping
@RequestMapping
```

这些就是 HTTP 路由。

## 第五步：看 Java Service

阅读：

```text
AiGenerationServiceImpl.java
AppVersionServiceImpl.java
```

目标：理解业务逻辑在哪里。

你要能说出：

```text
Controller 接请求，Service 做业务，Mapper 存数据库。
```

## 第六步：看 Python AI workflow

阅读：

```text
html_generation_workflow.py
html_generation_agents.py
```

目标：理解 AI 生成为什么拆成 planner/ui/code/fix/test。

## 第七步：看安全校验

阅读：

```text
doc/security-rules.md
doc/security-content-fixtures.json
projectFileSecurity.ts
html_sandbox.py
ProjectFileValidator.java
```

目标：理解三层安全为什么要一致。

这部分很重要，因为这是项目质量核心。

## 第八步：看部署服务

阅读：

```text
doc/deployment.md
DeploymentExecutor.java
DeploymentExecutorRouter.java
DockerDeploymentExecutor.java
GithubActionsDeploymentExecutor.java
KubernetesDeploymentExecutor.java
```

目标：理解策略模式和默认 dry-run。

## 第九步：看测试

阅读：

```text
frontend/src/utils/*.test.ts
ai-services/ai-orchestrator/tests/test_generation.py
backend/platform-service/src/test
backend/deploy-service/src/test
```

目标：学会通过测试理解系统行为。

测试最适合新手读，因为它直接告诉你：

```text
输入什么
期望输出什么
什么情况会失败
```

## 第十步：最后再看数据库和部署

阅读：

```text
doc/sql.md
infra/mysql/init.sql
infra/docker-compose.yml
```

目标：知道数据最终存在哪里，基础设施怎么启动。

## 最重要的部分

优先级最高：

1. 前端生成/保存/导出流程。
2. Java Controller + Service。
3. Python AI workflow。
4. 三层安全校验。
5. 部署 executor。

## 可以后学的部分

可以后学：

- Kubernetes 真实部署细节。
- GitHub Actions dispatch 细节。
- MinIO/RabbitMQ/Redis 后续扩展。
- Playwright browser sandbox 深层机制。

你现在最应该先做到的是：

```text
能从前端一个按钮，
一路追到后端 Controller，
再追到 Service，
再追到数据库或 Python 服务，
最后知道结果怎么回到页面。
```

只要你能做到这一步，就已经真正开始会读这个项目了。
