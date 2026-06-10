# ZeroCode AI Platform - 技术设计文档（TDD）

# 1. 前端技术栈

## 核心框架

* Vue 3
* TypeScript
* Vite

## UI

* TailwindCSS
* Arco Design

## 编辑器

* Monaco Editor
* GrapesJS

---

# 2. Java 平台层

## Spring Boot

负责：

* API
* 权限
* 用户
* 项目管理

---

## Redis

负责：

* 缓存
* 对话上下文
* 限流

---

## RabbitMQ

负责：

* AI 异步任务
* 项目构建任务

---

# 3. Python AI 层

## FastAPI

AI 服务入口。

---

## LangGraph

负责：

* Agent Workflow
* 多步骤推理

---

## PydanticAI

负责：

* Structured Output
* JSON Schema

---

## Playwright

负责：

* 自动测试
* 页面截图
* 自动修复

---

# 4. 沙箱设计

## iframe Sandbox

用于：

* HTML/CSS/JS

## Docker Sandbox

用于：

* React/Vue 项目

---

# 5. Prompt 工程

采用：

* System Prompt
* Few-shot
* JSON Schema
* Function Calling

避免：

* Markdown 提取
* 正则解析

---

# 6. Structured Output

统一采用：

response_format=json_schema

strict=true

禁止：

Markdown 代码块输出。
