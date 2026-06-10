# Security Rules

## 1. 目标

本文档记录 ZeroCode AI Platform 当前三层安全规则对照关系，降低前端、Python AI 层、Java 平台层规则漂移风险。

适用范围：

- 前端保存前校验：`frontend/src/utils/projectFileSecurity.ts`
- 前端预览清理：`frontend/src/utils/previewDocument.ts`
- Python 路径校验：`ai-services/ai-orchestrator/app/tools/project_security.py`
- Python sandbox：`ai-services/ai-orchestrator/app/tools/html_sandbox.py`
- Python 模型限制：`ai-services/ai-orchestrator/app/models/generated_project.py`
- Java 平台校验：`backend/platform-service/src/main/java/com/zerocode/platform/util/ProjectFileValidator.java`
- Java DTO 限制：`backend/platform-service/src/main/java/com/zerocode/platform/dto/`

## 2. 结构限制

| 规则 | 当前值 | 前端 | Python | Java |
| --- | --- | --- | --- | --- |
| 项目名长度 | 1 到 128 | 最大 128 | `GeneratedProject.project_name` | DTO + `ProjectFileValidator` |
| 项目类型 | `html` / `vue` / `react` | UI 类型选择 | `ProjectType` | `ProjectFileValidator` |
| 文件数量 | 1 到 100 | 最大 100 | `GeneratedProject.files` | DTO |
| 文件路径长度 | 1 到 500 | 最大 500 | `GeneratedFile.file_path` | DTO |
| 文件类型长度 | 1 到 32 | 最大 32 | `GeneratedFile.file_type` | DTO |
| 单文件内容长度 | 最大 200,000 | 最大 200,000 | `GeneratedFile.content` | DTO |

## 3. 路径规则

三层共同规则：

- `\` 统一归一化为 `/`。
- 禁止空路径。
- 禁止绝对路径。
- 禁止空路径段。
- 禁止 `.` 路径段。
- 禁止 `..` 路径段。
- 归一化后禁止重复路径。

当前实现：

- 前端常量：`frontend/src/utils/projectSecurityLimits.ts`
- Python 常量：`ai-services/ai-orchestrator/app/models/project_limits.py`
- Java 常量：`backend/platform-service/src/main/java/com/zerocode/platform/util/ProjectSecurityLimits.java`
- 前端：`normalizeProjectPath()`、`isSafeProjectPath()`、`validateProjectFiles()`
- Python：`normalize_project_path()`、`is_safe_project_path()`、`validate_project_file_paths()`
- Java：`safeProjectPath()`、`validateProjectFiles()`、`validateUniqueSafePaths()`

## 4. HTML 内容规则

三层共同规则：

- 禁止无 `src` 的内联 `<script>`。
- 允许本地 `<script src="...">`，用于 Vue/React Vite 入口。
- 禁止 HTML 内联事件处理器，例如 `onclick=...`。
- 禁止 HTML URL 属性引用外部地址。

受控 URL 属性：

- `src`
- `href`
- `action`
- `poster`

外部 URL 判定：

- `http://...`
- `https://...`
- `//...`

当前实现：

- 前端保存：`validateHtmlScriptTags()`、`INLINE_EVENT_HANDLER_PATTERN`、`EXTERNAL_HTML_URL_PATTERN`
- 前端预览：`sanitizePreviewMarkup()`
- Python sandbox：`HtmlInspectionParser`
- Java：`validateHtmlContent()`

## 5. CSS 内容规则

三层共同规则：

- 禁止 CSS 外部 URL。
- 禁止 `url(http://...)`。
- 禁止 `url(https://...)`。
- 禁止 `url(//...)`。
- 禁止 `@import url(...)` 引入外部 URL。

当前实现：

- 前端保存：`EXTERNAL_CSS_URL_PATTERN`
- 前端预览：`sanitizePreviewStyles()`
- Python sandbox：`EXTERNAL_CSS_URL_PATTERN`
- Java：`EXTERNAL_CSS_URL_PATTERN`

## 6. JS/TS/Vue/React 内容规则

受控文件类型：

- `js`
- `jsx`
- `ts`
- `tsx`
- `vue`

受控文件扩展名：

- `.js`
- `.jsx`
- `.ts`
- `.tsx`
- `.vue`

三层共同规则：

- 禁止网络请求 API：
  - `fetch(...)`
  - `XMLHttpRequest(...)`
  - `WebSocket(...)`
  - `EventSource(...)`
- 禁止动态代码执行：
  - `eval(...)`
  - `new Function(...)`
  - 字符串形式 `setTimeout("...")`
  - 字符串形式 `setInterval("...")`
- 允许函数回调形式 `setTimeout(() => ..., delay)`。

当前实现：

- 前端保存：`NETWORK_SCRIPT_PATTERN`、`DANGEROUS_SCRIPT_PATTERN`
- 前端预览：`sanitizePreviewScript()`
- Python sandbox：`run_static_sandbox_checks()`、`run_source_safety_checks()`
- Java：`validateFileContent()`

## 7. 预览规则

前端预览：

- 使用 iframe `sandbox="allow-scripts"`。
- 使用 `referrerpolicy="no-referrer"`。
- `srcdoc` 注入 CSP。
- 预览前清理 HTML/CSS/JS。
- CSS 注入前转义 `</style`。
- JS 注入前转义 `</script`。

Python Playwright 预览：

- `build_preview_document()` 注入同等 CSP。
- CSS 注入前转义 `</style`。
- JS 注入前转义 `</script`。
- 浏览器 sandbox 由 `ZEROCODE_ENABLE_BROWSER_SANDBOX=true` 启用；默认跳过。
- 启用后检查 body 文本非空。
- 启用后检查可见元素数量大于 0。
- 启用后生成 full-page screenshot，并检查截图字节非空。
- 若本地 Playwright 浏览器不可用，返回 `Browser sandbox: skipped (...)`，不阻断普通测试。

当前 CSP：

```text
default-src 'none';
img-src data: blob:;
style-src 'unsafe-inline';
script-src 'unsafe-inline';
connect-src 'none';
font-src data:;
media-src data: blob:;
```

## 8. Docker Sandbox 规则

Docker sandbox 用于 Vue/React 项目的真实构建隔离检查。

当前实现：

- `ai-services/ai-orchestrator/app/tools/docker_sandbox.py`
- 接入点：`ai-services/ai-orchestrator/app/agents/html_generation_agents.py` 的 `test_node`
- 适用项目类型：`vue`、`react`
- HTML 项目不需要 Docker sandbox，继续使用 iframe/Playwright 预览路径。
- Vue/React 生成模板和修复模板必须包含 `vite.config.ts`，并配置对应的 Vite 插件。

启用策略：

- 默认不启用，返回 `Docker sandbox: skipped`。
- 仅当 `ZEROCODE_ENABLE_DOCKER_SANDBOX=true` 时运行 Docker。
- 默认镜像为 `node:22-alpine`。
- 可通过 `ZEROCODE_DOCKER_SANDBOX_IMAGE` 覆盖镜像。

依赖安装阶段：

- 允许默认 Docker 网络，用于首次拉取 npm 依赖。
- 使用 `npm install --ignore-scripts`，禁止依赖安装脚本执行。
- 如果依赖安装失败或超时，返回 `Docker sandbox: skipped`，表示环境不可用，不判定生成项目失败。

构建阶段：

- 复用已安装依赖。
- 使用 `--network none` 执行 `npm run build`。
- 构建失败会返回 issue 并阻断工作流。

共同隔离参数：

- `--cpus 1`
- `--memory 512m`
- `--pids-limit 128`
- `--read-only`
- `--tmpfs /tmp:rw,noexec,nosuid,size=128m`
- 生成项目挂载到 `/workspace`

容器内命令：

```bash
npm_config_cache=/tmp/.npm npm install --ignore-scripts --no-audit --no-fund
npm run build
```

当前验证状态：

- Vue 生成骨架启用态验证通过：`Docker sandbox: build passed`
- React 生成骨架启用态验证通过：`Docker sandbox: build passed`

设计约束：

- 禁止在宿主机直接执行生成项目代码。
- Docker sandbox 启用态依赖本机 Docker daemon、镜像可用性和依赖缓存/网络策略。
- 当前 install 阶段允许网络；后续可以改为预构建依赖镜像或内部 npm 缓存，使全流程离线。

## 9. 已知风险

- 当前安全规则主要基于正则和 HTMLParser，不是完整 HTML/CSS/JS AST 安全分析。
- 前端、Python、Java 的结构限制值已抽取到各层命名常量；内容安全正则仍存在重复规则，修改任一层时必须同步检查另外两层。
- CSP 当前允许内联脚本，是为了支持 HTML 项目预览交互；风险由 iframe sandbox、CSP `connect-src 'none'`、内容清理和保存前校验共同降低。
- Docker sandbox 默认跳过，启用后才对 Vue/React 做真实构建隔离检查。
- 外部依赖版本和 npm audit 风险需要单独评估，不在本文档规则范围内。

## 10. 变更要求

修改任一安全规则时必须同步执行：

- 更新本文档。
- 更新 `doc/task-current.md`。
- 更新对应层测试。
- 至少执行受影响层测试。

推荐测试命令：

```bash
cd frontend && npm run test && npm run build
cd ai-services/ai-orchestrator && UV_CACHE_DIR=/private/tmp/uv-cache uv run pytest && UV_CACHE_DIR=/private/tmp/uv-cache uv run ruff check .
cd backend/platform-service && mvn -Dmaven.repo.local=/private/tmp/zerocode-m2 test
```
