# Git Repository

## 1. 初始化状态

当前项目已在根目录初始化 Git 仓库：

```bash
git init -b main
```

当前默认分支：

```text
main
```

当前仓库已创建首次提交并推送到 GitHub。

远端仓库：

```text
https://github.com/pppisnew/zeroCode-ai-platform.git
```

本地 `main` 已设置为跟踪 `origin/main`。

## 2. 忽略规则

根目录已存在 `.gitignore`，当前覆盖以下主要类别：

- macOS 系统文件：`.DS_Store`
- Node/Vite 依赖与构建产物：`node_modules/`、`dist/`
- Java/Maven 构建产物：`target/`
- IDE 配置：`.idea/`、`.vscode/`
- 环境与缓存：`.env`、`.venv/`、`__pycache__/`、`.pytest_cache/`、`.ruff_cache/`
- 日志：`*.log`

仓库级 `.gitignore` 显式放行 Python 包标记文件：

```text
!**/__init__.py
```

原因是当前机器存在用户级全局忽略规则 `/Users/apple/.gitignore_global`，其中 `__*` 会误忽略 `__init__.py`。仓库必须覆盖该规则，避免 Python 包文件漏提交。

前端目录 `frontend/.gitignore` 也保留 Vite 默认忽略规则。

## 3. 建议首次提交流程

后续提交前建议执行：

```bash
git status --short
```

确认不包含以下目录或文件：

- `frontend/node_modules`
- `frontend/dist`
- `backend/*/target`
- `ai-services/ai-orchestrator/.venv`
- `.DS_Store`
- `.env`

确认后执行：

```bash
git add .
git status --short
git commit -m "<type>: <summary>"
git push
```

## 4. 当前注意事项

- 首次提交已完成并推送到 `origin/main`。
- 后续创建提交前，应先确认生成产物和本地环境文件没有被纳入暂存区。
- 当前任务状态仍以 `doc/task-current.md` 作为恢复入口。
