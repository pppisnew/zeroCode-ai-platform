from __future__ import annotations

import json
from collections.abc import Callable
from dataclasses import dataclass

from app.models.generated_project import GeneratedFile
from app.models.project_type import ProjectType


@dataclass(frozen=True)
class RepairResult:
    files: list[GeneratedFile]
    report: list[str]


def repair_project_files(files: list[GeneratedFile], project_type: ProjectType) -> RepairResult:
    if project_type == "html":
        return RepairResult(files=files, report=[])

    repaired_files = [file.model_copy() for file in files]
    report: list[str] = []
    _ensure_required_files(repaired_files, project_type, report)
    _repair_required_file_content(repaired_files, project_type, report)

    package_file = _find_file(repaired_files, "package.json")
    if package_file is None:
        raise ValueError("package.json repair failed")
    package_json, parse_report = repair_package_json(package_file.content, project_type)
    package_file.content = package_json
    report.extend(parse_report)

    return RepairResult(files=repaired_files, report=report)


def _ensure_required_files(
    files: list[GeneratedFile],
    project_type: ProjectType,
    report: list[str],
) -> None:
    for file in _required_file_templates(project_type):
        if _find_file(files, file.file_path) is None:
            files.append(file)
            report.append(f"{file.file_path}: created")


def _repair_required_file_content(
    files: list[GeneratedFile],
    project_type: ProjectType,
    report: list[str],
) -> None:
    templates = {file.file_path: file for file in _required_file_templates(project_type)}
    if project_type == "vue":
        _replace_if_invalid(
            files,
            templates,
            "index.html",
            lambda content: '<script type="module" src="/src/main.ts"></script>' in content,
            report,
        )
        _replace_if_invalid(
            files,
            templates,
            "vite.config.ts",
            lambda content: "@vitejs/plugin-vue" in content,
            report,
        )
        _replace_if_invalid(
            files,
            templates,
            "src/main.ts",
            lambda content: 'import App from "./App.vue"' in content,
            report,
        )
        _replace_if_invalid(
            files,
            templates,
            "src/App.vue",
            lambda content: "<template>" in content and "</template>" in content,
            report,
        )
    if project_type == "react":
        _replace_if_invalid(
            files,
            templates,
            "index.html",
            lambda content: '<script type="module" src="/src/main.tsx"></script>' in content,
            report,
        )
        _replace_if_invalid(
            files,
            templates,
            "vite.config.ts",
            lambda content: "@vitejs/plugin-react" in content,
            report,
        )
        _replace_if_invalid(
            files,
            templates,
            "src/main.tsx",
            lambda content: 'import { App } from "./App"' in content,
            report,
        )
        _replace_if_invalid(
            files,
            templates,
            "src/App.tsx",
            lambda content: "export function App" in content,
            report,
        )


def _replace_if_invalid(
    files: list[GeneratedFile],
    templates: dict[str, GeneratedFile],
    file_path: str,
    is_valid: Callable[[str], bool],
    report: list[str],
) -> None:
    file = _find_file(files, file_path)
    template = templates[file_path]
    if file is not None and not is_valid(file.content):
        file.content = template.content
        file.file_type = template.file_type
        report.append(f"{file_path}: repaired")


def _find_file(files: list[GeneratedFile], file_path: str) -> GeneratedFile | None:
    return next((file for file in files if file.file_path == file_path), None)


def repair_package_json(content: str, project_type: ProjectType) -> tuple[str, list[str]]:
    report: list[str] = []
    try:
        package_data = json.loads(content or "{}")
    except json.JSONDecodeError:
        package_data = {}
        report.append("package.json: replaced invalid JSON")

    if not isinstance(package_data, dict):
        package_data = {}
        report.append("package.json: replaced non-object JSON")

    package_data.setdefault("scripts", {})
    if not isinstance(package_data["scripts"], dict):
        package_data["scripts"] = {}
        report.append("package.json scripts: repaired")

    scripts = package_data["scripts"]
    if scripts.get("dev") != "vite":
        scripts["dev"] = "vite"
        report.append("package.json scripts.dev: repaired")
    if scripts.get("build") != "vite build":
        scripts["build"] = "vite build"
        report.append("package.json scripts.build: repaired")

    package_data.setdefault("dependencies", {})
    if not isinstance(package_data["dependencies"], dict):
        package_data["dependencies"] = {}
        report.append("package.json dependencies: repaired")

    dependencies = package_data["dependencies"]
    required_dependencies = _required_dependencies(project_type)
    for name, version in required_dependencies.items():
        if dependencies.get(name) != version:
            dependencies[name] = version
            report.append(f"package.json dependency {name}: repaired")

    package_data.setdefault("devDependencies", {})
    if not isinstance(package_data["devDependencies"], dict):
        package_data["devDependencies"] = {}
        report.append("package.json devDependencies: repaired")

    return json.dumps(package_data, ensure_ascii=False, sort_keys=True), report


def _required_dependencies(project_type: ProjectType) -> dict[str, str]:
    if project_type == "vue":
        return {
            "@vitejs/plugin-vue": "latest",
            "vite": "latest",
            "vue": "latest",
        }
    if project_type == "react":
        return {
            "@vitejs/plugin-react": "latest",
            "vite": "latest",
            "react": "latest",
            "react-dom": "latest",
        }
    return {}


def _required_file_templates(project_type: ProjectType) -> list[GeneratedFile]:
    if project_type == "vue":
        return [
            GeneratedFile(filePath="package.json", fileType="json", content="{}"),
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
                    "      <h1>Generated Vue App</h1>\n"
                    "      <p>这是 Vue 项目的结构化修复骨架。</p>\n"
                    "    </section>\n"
                    "  </main>\n"
                    "</template>\n"
                    '<style src="./style.css"></style>\n'
                ),
            ),
            GeneratedFile(filePath="src/style.css", fileType="css", content=_shared_app_css()),
        ]
    if project_type == "react":
        return [
            GeneratedFile(filePath="package.json", fileType="json", content="{}"),
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
                    "        <h1>Generated React App</h1>\n"
                    "        <p>这是 React 项目的结构化修复骨架。</p>\n"
                    "      </section>\n"
                    "    </main>\n"
                    "  )\n"
                    "}\n"
                ),
            ),
            GeneratedFile(filePath="src/style.css", fileType="css", content=_shared_app_css()),
        ]
    return []


def _shared_app_css() -> str:
    return (
        "body{margin:0;font-family:Inter,system-ui;background:#eef2ff;color:#172033}"
        ".app-shell{min-height:100vh;display:grid;place-items:center;padding:24px}"
        "section{max-width:680px;background:white;border:1px solid #dbe3f0;"
        "border-radius:8px;padding:32px;box-shadow:0 20px 50px rgba(15,23,42,.08)}"
        "span{color:#2563eb;font-weight:700;font-size:13px}"
        "h1{font-size:32px;line-height:1.15;margin:12px 0;color:#111827}"
        "p{color:#475569;line-height:1.7}"
    )
