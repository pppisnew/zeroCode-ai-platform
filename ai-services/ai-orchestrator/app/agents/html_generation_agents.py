from dataclasses import dataclass
from html import escape
from typing import NotRequired, TypedDict

from app.models.generated_project import GeneratedFile, GeneratedProject
from app.models.project_type import ProjectType
from app.prompts.html_generation_prompts import (
    CODE_SYSTEM_PROMPT,
    PLANNER_SYSTEM_PROMPT,
    UI_SYSTEM_PROMPT,
    build_ui_summary,
    project_name_for_type,
)
from app.services.dependencies import get_llm_client
from app.services.llm_client import LlmClient
from app.tools.docker_sandbox import run_docker_sandbox_checks
from app.tools.html_sandbox import run_html_sandbox_checks, run_source_safety_checks
from app.tools.project_repair import repair_project_files
from app.tools.project_security import validate_project_file_paths


class HtmlGenerationState(TypedDict):
    prompt: str
    project_type: ProjectType
    base_project: NotRequired[GeneratedProject | None]
    project_name: NotRequired[str]
    ui_summary: NotRequired[str]
    files: NotRequired[list[GeneratedFile]]
    repair_report: NotRequired[list[str]]
    issues: NotRequired[list[str]]
    test_report: NotRequired[list[str]]
    workflow_steps: NotRequired[list[str]]


@dataclass(frozen=True)
class SkeletonCheckResult:
    issues: list[str]
    report: list[str]


def planner_node(state: HtmlGenerationState) -> HtmlGenerationState:
    normalized_prompt = state["prompt"].strip()
    base_project = state.get("base_project")
    project_type = base_project.project_type if base_project else state["project_type"]

    llm: LlmClient = get_llm_client()
    user_prompt = (
        f"User requirement: {normalized_prompt}\n"
        f"Current project type: {project_type}\n\n"
        "Analyze the requirement and output a short project plan (1-2 sentences in Chinese). "
        "Describe what pages/components the project needs and the key features."
    )
    try:
        plan_text = llm.chat(PLANNER_SYSTEM_PROMPT, user_prompt)
    except RuntimeError:
        plan_text = f"Generate a {project_type} project for: {normalized_prompt}"

    return {
        **state,
        "prompt": normalized_prompt,
        "project_type": project_type,
        "project_name": project_name_for_type(project_type),
        "ui_summary": plan_text,
        "workflow_steps": _record_step(state, "planner"),
    }


def ui_node(state: HtmlGenerationState) -> HtmlGenerationState:
    planner_output = state.get("ui_summary", state["prompt"])
    project_type = state["project_type"]

    llm: LlmClient = get_llm_client()
    user_prompt = (
        f"Project type: {project_type}\n"
        f"Planner analysis: {planner_output}\n\n"
        "Design the UI structure. Output a concise description of:\n"
        "1. The layout structure (header, main, footer, sidebar, etc.)\n"
        "2. Key visual components (buttons, forms, lists, cards, etc.)\n"
        "3. Color scheme and visual style\n"
        "Keep the output under 200 words."
    )
    try:
        ui_summary = llm.chat(UI_SYSTEM_PROMPT, user_prompt)
    except RuntimeError:
        ui_summary = build_ui_summary(project_type, state["prompt"])

    return {
        **state,
        "ui_summary": ui_summary,
        "workflow_steps": _record_step(state, "ui"),
    }


def code_node(state: HtmlGenerationState) -> HtmlGenerationState:
    prompt = state["prompt"]
    ui_summary = state.get("ui_summary", "")
    project_type = state["project_type"]
    base_project = state.get("base_project")

    # Conversational update: modify existing project via LLM
    if base_project is not None:
        try:
            files = _llm_conversational_update(base_project.files, prompt, project_type)
        except RuntimeError:
            files = _apply_conversational_update(base_project.files, prompt, project_type)
        return {
            **state,
            "files": files,
            "workflow_steps": _record_step(state, "code"),
        }

    # New generation: use LLM structured output or fallback to templates
    try:
        files = _llm_generate_files(prompt, ui_summary, project_type)
    except RuntimeError:
        files = _template_generate_files(prompt, project_type)

    return {
        **state,
        "files": files,
        "workflow_steps": _record_step(state, "code"),
    }


def fix_node(state: HtmlGenerationState) -> HtmlGenerationState:
    repair_result = repair_project_files(state.get("files", []), state["project_type"])
    files = repair_result.files
    issues = []
    if state["project_type"] == "vue":
        required_paths = {
            "package.json",
            "vite.config.ts",
            "index.html",
            "src/main.ts",
            "src/App.vue",
            "src/style.css",
        }
    elif state["project_type"] == "react":
        required_paths = {
            "package.json",
            "vite.config.ts",
            "index.html",
            "src/main.tsx",
            "src/App.tsx",
            "src/style.css",
        }
    else:
        required_paths = {"index.html", "style.css", "script.js"}
    actual_paths = {file.file_path for file in files}
    missing_paths = required_paths - actual_paths
    # Create missing required files with sensible defaults
    for missing in sorted(missing_paths):
        if missing == "style.css":
            files.append(GeneratedFile(filePath="style.css", fileType="css", content="/* Generated styles */"))
        elif missing == "script.js":
            files.append(GeneratedFile(filePath="script.js", fileType="js", content="// Generated script"))
        else:
            issues.append(f"Missing file: {missing}")

    return {
        **state,
        "files": files,
        "issues": issues,
        "repair_report": repair_result.report,
        "workflow_steps": _record_step(state, "fix"),
    }


def test_node(state: HtmlGenerationState) -> HtmlGenerationState:
    existing_issues = state.get("issues", [])
    path_result = validate_project_file_paths(state.get("files", []))
    if state["project_type"] == "html":
        sandbox_result = run_html_sandbox_checks(state.get("files", []))
    else:
        skeleton_result = _run_project_skeleton_checks(
            state.get("files", []),
            state["project_type"],
        )
        source_result = run_source_safety_checks(state.get("files", []))
        docker_result = run_docker_sandbox_checks(state.get("files", []), state["project_type"])
        sandbox_result = SkeletonCheckResult(
            issues=[*skeleton_result.issues, *source_result.issues, *docker_result.issues],
            report=[*skeleton_result.report, *source_result.report, *docker_result.report],
        )
    issues = [*existing_issues, *path_result.issues, *sandbox_result.issues]
    if issues:
        import logging
        logging.getLogger(__name__).error("Workflow checks failed: %s", issues)
        raise ValueError("Generated project failed workflow checks")
    return {
        **state,
        "issues": issues,
        "test_report": [
            *state.get("repair_report", []),
            *path_result.report,
            *sandbox_result.report,
        ],
        "workflow_steps": _record_step(state, "test"),
    }


# ---------------------------------------------------------------------------
# LLM-powered generation
# ---------------------------------------------------------------------------

def _llm_generate_files(
    prompt: str,
    ui_summary: str,
    project_type: ProjectType,
) -> list[GeneratedFile]:
    llm: LlmClient = get_llm_client()
    system_prompt = (
        f"{CODE_SYSTEM_PROMPT}\n\n"
        f"Project type: {project_type}\n"
        "You MUST return a valid GeneratedProject JSON with all required files for a "
        f"complete {project_type} project. Include all necessary config files, "
        "entry points, and components. The code must be sandbox-safe and production-ready."
    )
    user_prompt = (
        f"UI Structure Design:\n{ui_summary}\n\n"
        f"User Requirement:\n{prompt}\n\n"
        f"Generate a complete {project_type} project that implements the requirement above. "
        "Include proper styling, interactions, and a polished UI."
    )
    result = llm.chat_structured(system_prompt, user_prompt, GeneratedProject)
    return result.files


def _llm_conversational_update(
    files: list[GeneratedFile],
    prompt: str,
    project_type: ProjectType,
) -> list[GeneratedFile]:
    llm: LlmClient = get_llm_client()
    existing_summary = "\n".join(
        f"  {f.file_path} ({f.file_type}): {len(f.content)} chars"
        for f in files
    )
    system_prompt = (
        "You are a code modification agent. Given an existing project's files and a "
        "modification request, return the complete updated project. Preserve all "
        "existing functionality that is not explicitly asked to change. "
        "Return structured output matching GeneratedProject exactly."
    )
    user_prompt = (
        f"Project type: {project_type}\n\n"
        f"Existing files:\n{existing_summary}\n\n"
        f"Modification request: {prompt}\n\n"
        "Return the COMPLETE updated project with all files. "
        "Apply the modification intelligently — rewrite relevant files, "
        "not just append content."
    )
    # Build the full project from existing files for the structured LLM call
    existing_project = GeneratedProject(
        projectName="update",
        projectType=project_type,
        files=files,
    )
    project_json = existing_project.model_dump_json(by_alias=True)
    full_prompt = f"{user_prompt}\n\nCurrent project JSON:\n{project_json}"
    result = llm.chat_structured(system_prompt, full_prompt, GeneratedProject)
    return result.files


# ---------------------------------------------------------------------------
# Template-based fallback generation (used when LLM is unavailable)
# ---------------------------------------------------------------------------

def _template_generate_files(prompt: str, project_type: ProjectType) -> list[GeneratedFile]:
    if project_type == "vue":
        return _build_vue_files(prompt)
    if project_type == "react":
        return _build_react_files(prompt)
    return _build_html_files(prompt)


def _apply_conversational_update(
    files: list[GeneratedFile],
    prompt: str,
    project_type: ProjectType,
) -> list[GeneratedFile]:
    updated_files = [file.model_copy() for file in files]
    target_file_path = _conversation_target_file(project_type)
    target_file = next(
        (file for file in updated_files if file.file_path == target_file_path), None
    )
    update_block = _build_conversation_update_block(prompt, project_type)
    if target_file is not None:
        target_file.content = _insert_conversation_update(
            target_file.content, update_block, project_type
        )
        _ensure_update_note_styles(updated_files)
        return updated_files
    updated_files.append(
        GeneratedFile(
            filePath=target_file_path,
            fileType=_file_type_for_path(target_file_path),
            content=update_block,
        ),
    )
    _ensure_update_note_styles(updated_files)
    return updated_files


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------

def _record_step(state: HtmlGenerationState, step: str) -> list[str]:
    return [*state.get("workflow_steps", []), step]


# ---------------------------------------------------------------------------
# Template file builders (fallback when LLM is unavailable)
# ---------------------------------------------------------------------------

def _build_html_files(prompt: str) -> list[GeneratedFile]:
    return [
        GeneratedFile(
            filePath="index.html",
            fileType="html",
            content=(
                '<main class="app-shell">'
                "<section>"
                "<span>ZeroCode MVP</span>"
                f"<h1>{escape(prompt, quote=True)}</h1>"
                "<p>这是 AI 编排服务的结构化 HTML 生成占位结果。</p>"
                "<p>请设置 LLM_API_KEY 环境变量以启用真实 AI 生成。</p>"
                '<button id="primaryAction">开始使用</button>'
                "</section>"
                "</main>"
            ),
        ),
        GeneratedFile(
            filePath="style.css",
            fileType="css",
            content=(
                "body{margin:0;font-family:Inter,system-ui;background:#eef2ff;color:#172033}"
                ".app-shell{min-height:100vh;display:grid;place-items:center;padding:24px}"
                "section{max-width:680px;background:white;border:1px solid #dbe3f0;"
                "border-radius:8px;padding:32px;box-shadow:0 20px 50px rgba(15,23,42,.08)}"
                "span{color:#2563eb;font-weight:700;font-size:13px}"
                "h1{font-size:32px;line-height:1.15;margin:12px 0;color:#111827}"
                "p{color:#475569;line-height:1.7}"
                "button{border:0;border-radius:6px;background:#2563eb;color:white;"
                "padding:12px 16px;font-weight:700}"
            ),
        ),
        GeneratedFile(
            filePath="script.js",
            fileType="js",
            content=(
                'document.getElementById("primaryAction")?.addEventListener("click",()=>{'
                'console.log("ZeroCode generated app action")'
                "})"
            ),
        ),
    ]


def _build_vue_files(prompt: str) -> list[GeneratedFile]:
    safe_prompt = escape(prompt, quote=True)
    return [
        GeneratedFile(
            filePath="package.json",
            fileType="json",
            content=(
                '{"scripts":{"dev":"vite","build":"vite build"},'
                '"dependencies":{"@vitejs/plugin-vue":"^5.2.0","vite":"^6.0.0","vue":"^3.5.0"},'
                '"devDependencies":{}}'
            ),
        ),
        GeneratedFile(
            filePath="index.html",
            fileType="html",
            content='<div id="app"></div><script type="module" src="/src/main.ts"></script>',
        ),
        GeneratedFile(
            filePath="vite.config.ts",
            fileType="ts",
            content=(
                'import { defineConfig } from "vite"\n'
                'import vue from "@vitejs/plugin-vue"\n\n'
                "export default defineConfig({\n"
                "  plugins: [vue()],\n"
                "})\n"
            ),
        ),
        GeneratedFile(
            filePath="src/main.ts",
            fileType="ts",
            content=(
                'import { createApp } from "vue"\n'
                'import App from "./App.vue"\n'
                'createApp(App).mount("#app")\n'
            ),
        ),
        GeneratedFile(
            filePath="src/App.vue",
            fileType="vue",
            content=(
                "<template>\n"
                '  <main class="app-shell">\n'
                "    <section>\n"
                "      <span>ZeroCode Vue</span>\n"
                f"      <h1>{safe_prompt}</h1>\n"
                "      <p>这是 Vue 项目的生成骨架（未配置 LLM）。</p>\n"
                '      <button type="button">开始使用</button>\n'
                "    </section>\n"
                "  </main>\n"
                "</template>\n"
                '<style src="./style.css"></style>\n'
            ),
        ),
        GeneratedFile(
            filePath="src/style.css",
            fileType="css",
            content=_shared_app_css(),
        ),
    ]


def _build_react_files(prompt: str) -> list[GeneratedFile]:
    safe_prompt = escape(prompt, quote=True)
    return [
        GeneratedFile(
            filePath="package.json",
            fileType="json",
            content=(
                '{"scripts":{"dev":"vite","build":"vite build"},'
                '"dependencies":{"@vitejs/plugin-react":"^4.3.0","vite":"^6.0.0",'
                '"react":"^18.3.0","react-dom":"^18.3.0"},'
                '"devDependencies":{}}'
            ),
        ),
        GeneratedFile(
            filePath="index.html",
            fileType="html",
            content='<div id="root"></div><script type="module" src="/src/main.tsx"></script>',
        ),
        GeneratedFile(
            filePath="vite.config.ts",
            fileType="ts",
            content=(
                'import { defineConfig } from "vite"\n'
                'import react from "@vitejs/plugin-react"\n\n'
                "export default defineConfig({\n"
                "  plugins: [react()],\n"
                "})\n"
            ),
        ),
        GeneratedFile(
            filePath="src/main.tsx",
            fileType="tsx",
            content=(
                'import React from "react"\n'
                'import { createRoot } from "react-dom/client"\n'
                'import "./style.css"\n'
                "import { App } from \"./App\"\n\n"
                'createRoot(document.getElementById("root")!).render(<App />)\n'
            ),
        ),
        GeneratedFile(
            filePath="src/App.tsx",
            fileType="tsx",
            content=(
                "export function App() {\n"
                "  return (\n"
                '    <main className="app-shell">\n'
                "      <section>\n"
                "        <span>ZeroCode React</span>\n"
                f"        <h1>{safe_prompt}</h1>\n"
                "        <p>这是 React 项目的生成骨架（未配置 LLM）。</p>\n"
                '        <button type="button">开始使用</button>\n'
                "      </section>\n"
                "    </main>\n"
                "  )\n"
                "}\n"
            ),
        ),
        GeneratedFile(
            filePath="src/style.css",
            fileType="css",
            content=_shared_app_css(),
        ),
    ]


def _shared_app_css() -> str:
    return (
        "body{margin:0;font-family:Inter,system-ui;background:#eef2ff;color:#172033}"
        ".app-shell{min-height:100vh;display:grid;place-items:center;padding:24px}"
        "section{max-width:680px;background:white;border:1px solid #dbe3f0;"
        "border-radius:8px;padding:32px;box-shadow:0 20px 50px rgba(15,23,42,.08)}"
        "span{color:#2563eb;font-weight:700;font-size:13px}"
        "h1{font-size:32px;line-height:1.15;margin:12px 0;color:#111827}"
        "p{color:#475569;line-height:1.7}"
        "button{border:0;border-radius:6px;background:#2563eb;color:white;"
        "padding:12px 16px;font-weight:700}"
    )


# ---------------------------------------------------------------------------
# Conversational update helpers (fallback when LLM unavailable)
# ---------------------------------------------------------------------------

def _build_conversation_update_block(prompt: str, project_type: ProjectType) -> str:
    safe_prompt = escape(prompt, quote=True)
    if project_type == "react":
        return (
            '<section className="update-note">\n'
            "          <span>Conversation update</span>\n"
            f"          <p>{safe_prompt}</p>\n"
            "        </section>"
        )
    return (
        '<section class="update-note">\n'
        "        <span>Conversation update</span>\n"
        f"        <p>{safe_prompt}</p>\n"
        "      </section>"
    )


def _insert_conversation_update(
    content: str,
    update_block: str,
    project_type: ProjectType,
) -> str:
    if project_type == "vue" and "</main>" in content:
        return content.replace("</main>", f"    {update_block}\n  </main>", 1)
    if project_type == "react" and "</main>" in content:
        return content.replace("</main>", f"      {update_block}\n    </main>", 1)
    if "</main>" in content:
        return content.replace("</main>", f"{update_block}</main>", 1)
    return f"{content}\n{update_block}"


def _ensure_update_note_styles(files: list[GeneratedFile]) -> None:
    css_file = next(
        (file for file in files if file.file_path.endswith("style.css")), None
    )
    update_css = (
        ".update-note{margin-top:18px;border:1px solid #bfdbfe;"
        "background:#eff6ff;border-radius:8px;padding:16px}"
        ".update-note span{display:block;color:#1d4ed8;font-weight:700;font-size:12px}"
        ".update-note p{margin:8px 0 0;color:#1e293b}"
    )
    if css_file is None:
        files.append(GeneratedFile(filePath="style.css", fileType="css", content=update_css))
        return
    if ".update-note" not in css_file.content:
        css_file.content = f"{css_file.content}{update_css}"


def _conversation_target_file(project_type: ProjectType) -> str:
    if project_type == "vue":
        return "src/App.vue"
    if project_type == "react":
        return "src/App.tsx"
    return "index.html"


def _file_type_for_path(file_path: str) -> str:
    if file_path.endswith(".vue"):
        return "vue"
    if file_path.endswith(".tsx"):
        return "tsx"
    if file_path.endswith(".html"):
        return "html"
    return "txt"


# ---------------------------------------------------------------------------
# Skeleton checks (unchanged from original)
# ---------------------------------------------------------------------------

def _run_project_skeleton_checks(
    files: list[GeneratedFile],
    project_type: ProjectType,
) -> SkeletonCheckResult:
    issues: list[str] = []
    report: list[str] = []
    package_file = _find_file(files, "package.json")
    vite_config_file = _find_file(files, "vite.config.ts")
    index_file = _find_file(files, "index.html")
    if package_file is None:
        issues.append("package.json is missing")
    else:
        report.append("package.json: present")
    if vite_config_file is None:
        issues.append("vite.config.ts is missing")
    elif project_type == "vue" and "@vitejs/plugin-vue" not in vite_config_file.content:
        issues.append("vite.config.ts does not configure Vue plugin")
    elif project_type == "react" and "@vitejs/plugin-react" not in vite_config_file.content:
        issues.append("vite.config.ts does not configure React plugin")
    else:
        report.append("Vite plugin config: present")
    if index_file is None or '<script type="module"' not in index_file.content:
        issues.append("index.html does not reference a module entry")
    else:
        report.append("module entry: present")
    if project_type == "vue":
        _check_vue_entry(files, issues, report)
    if project_type == "react":
        _check_react_entry(files, issues, report)
    return SkeletonCheckResult(issues=issues, report=report)


def _check_vue_entry(
    files: list[GeneratedFile],
    issues: list[str],
    report: list[str],
) -> None:
    main_file = _find_file(files, "src/main.ts")
    app_file = _find_file(files, "src/App.vue")
    if main_file is None:
        issues.append("src/main.ts is missing")
    elif 'import App from "./App.vue"' not in main_file.content:
        issues.append("src/main.ts does not import App.vue")
    else:
        report.append("Vue entry import: present")
    if app_file is None:
        issues.append("src/App.vue is missing")
    elif "<template>" not in app_file.content or "</template>" not in app_file.content:
        issues.append("src/App.vue template is missing")
    else:
        report.append("Vue app template: present")


def _check_react_entry(
    files: list[GeneratedFile],
    issues: list[str],
    report: list[str],
) -> None:
    main_file = _find_file(files, "src/main.tsx")
    app_file = _find_file(files, "src/App.tsx")
    if main_file is None:
        issues.append("src/main.tsx is missing")
    elif 'import { App } from "./App"' not in main_file.content:
        issues.append("src/main.tsx does not import App")
    else:
        report.append("React entry import: present")
    if app_file is None:
        issues.append("src/App.tsx is missing")
    elif "export function App" not in app_file.content:
        issues.append("src/App.tsx does not export App")
    else:
        report.append("React app export: present")


def _find_file(files: list[GeneratedFile], file_path: str) -> GeneratedFile | None:
    return next((file for file in files if file.file_path == file_path), None)
