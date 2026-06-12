# Phase 0: AI 核心链路修复

**日期**: 2026-06-12 ~ 2026-06-13
**状态**: 已完成
**对应文档**: AGENTS.md §5, §8, §10, §18

## 目标

将 AI 生成从硬编码模板替换为真实 LLM 调用（DeepSeek），使系统符合 AGENTS.md §18 要求："AI MUST NEVER generate placeholder TODO implementations"。

## 问题诊断

1. **.env 未配置 LLM 密钥**：用户将密钥写在 `.env.example`（模板文件），实际加载的 `.env` 无 LLM 配置
2. **DeepSeek 不兼容 OpenAI `json_schema` strict 模式**：启动时报 `This response_format type is unavailable now`
3. **HTTP 超时太短**：`AiServiceConfig.java` 的 readTimeout 仅 20s，LLM 生成需要 30-90s
4. **LLM 字段名不匹配**：DeepSeek 生成 `{"path": ..., "content": ...}` 而非 `{"filePath": ..., "fileType": ..., "content": ...}`
5. **LLM 生成内联样式**：DeepSeek 倾向在 HTML 中内联 `<style>` 和 `<script>`，而非独立文件

## 修复内容

### 1. `.env` 配置
- 添加 `LLM_PROVIDER=openai`
- 添加 `LLM_API_KEY=sk-...`（DeepSeek API key）
- 添加 `LLM_MODEL=deepseek-v4-pro`
- 添加 `LLM_BASE_URL=https://api.deepseek.com`

### 2. DeepSeek 兼容性修复 (`llm_client.py`)
- 三层 fallback 策略：
  1. `json_schema` strict 模式（OpenAI 原生）→ 失败
  2. `json_object` 模式 → 失败（DeepSeek 也不支持）
  3. 纯文本 + prompt 指令 + JSON 提取 → 成功
- 新增 `_extract_json()` 函数：去除 markdown fence，提取 JSON 对象
- 新增 `_format_schema_hint()` 函数：用 aliased field names 生成可读 schema
- JSON 校验失败时自动重试（将 validation errors 反馈给 LLM）

### 3. HTTP 超时修复 (`AiServiceConfig.java`)
- `readTimeout`: 20s → 120s（适配 LLM 生成延迟）

### 4. Prompt 优化 (`html_generation_prompts.py`)
- `CODE_SYSTEM_PROMPT` 增加明确字段名要求：
  - "Each file MUST have filePath (NOT 'path'), fileType (NOT 'type')"
  - 明确列出完整 JSON 结构示例
  - 强调 sandbox-safe 规则（禁止 inline event handlers 等）

### 5. 缺失文件修复 (`html_generation_agents.py`)
- `fix_node` 在检测到缺失 `style.css` / `script.js` 时自动创建默认文件（而非仅报告错误）

### 6. 依赖版本修复 (`html_generation_agents.py`)
- Vue 模板 `package.json`: `"latest"` → `"^5.2.0"`, `"^6.0.0"`, `"^3.5.0"`
- React 模板 `package.json`: `"latest"` → `"^4.3.0"`, `"^6.0.0"`, `"^18.3.0"`

## 验证结果

```bash
# 不同 prompt 生成不同内容
curl -X POST :8000/generations/html -d '{"prompt":"Hello World页面","projectType":"html"}'
# → index.html (702 chars) + style.css (1714 chars) + script.js (768 chars)

curl -X POST :8000/generations/html -d '{"prompt":"Todo应用","projectType":"html"}'
# → index.html (完全不同的内容) + style.css + script.js

# 全量测试
uv run python -m pytest  # 47 passed
```

## 变更文件

| 文件 | 变更 |
|------|------|
| `ai-services/ai-orchestrator/app/services/llm_client.py` | DeepSeek 兼容三层 fallback + JSON 提取 + 重试 |
| `ai-services/ai-orchestrator/app/prompts/html_generation_prompts.py` | CODE_SYSTEM_PROMPT 增加字段名和示例 |
| `ai-services/ai-orchestrator/app/agents/html_generation_agents.py` | fix_node 自动创建缺失文件 + 版本号修复 |
| `backend/platform-service/.../AiServiceConfig.java` | readTimeout 20s → 120s |
| `.env` | 添加 LLM 配置（不入 git） |

## 残留问题

- `json_schema` strict 模式每次首次调用仍会报 warning 并 fallback（DeepSeek 不支持）
- LLM 偶尔生成不符合安全规则的代码（inline event handlers），被 test_node 拒绝后返回 400
- 修复策略：已通过 `fix_node` 自动补全缺失文件，通过 prompt 减少违规生成
