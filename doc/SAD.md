# ZeroCode AI Platform - 系统架构文档（SAD）

# 1. 总体架构

系统采用：

Java + Python 微服务架构。

---

# 2. 架构分层

## 2.1 前端层

技术：

* Vue3
* TypeScript
* Monaco Editor
* GrapesJS

职责：

* AI 对话
* 代码编辑
* 实时预览
* 可视化编辑

---

## 2.2 平台层（Java）

技术：

* Spring Boot 3
* Redis
* RabbitMQ
* MySQL
* MinIO

职责：

* 用户系统
* 项目管理
* 权限
* 文件管理
* 消息队列
* API Gateway

---

## 2.3 AI 服务层（Python）

技术：

* FastAPI
* LangGraph
* PydanticAI
* Playwright

职责：

* Prompt 编排
* Agent 工作流
* 代码生成
* 自动修复
* 自动测试

---

## 2.4 沙箱层

技术：

* iframe Sandbox
* Docker Sandbox
* WebContainer（未来）

职责：

* 运行生成代码
* 隔离执行环境

---

# 3. 微服务通信

采用：

* HTTP REST
* RabbitMQ
* WebSocket

---

# 4. AI 工作流

用户请求：

需求
→ Planner Agent
→ UI Agent
→ Code Agent
→ Fix Agent
→ Sandbox Test
→ 返回结果

---

# 5. 存储架构

## MySQL

存储：

* 用户
* 项目
* 版本
* 消息

## Redis

存储：

* Session
* 缓存
* Agent 状态

## MinIO

存储：

* 项目 ZIP
* 代码快照
* 静态资源
