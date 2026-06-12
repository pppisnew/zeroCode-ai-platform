import json
from pathlib import Path

from fastapi.testclient import TestClient
from pydantic import ValidationError

import app.tools.html_sandbox as html_sandbox
from app.main import app
from app.models.generated_project import GeneratedFile, GeneratedProject
from app.models.generation_request import GenerateHtmlRequest
from app.prompts.html_generation_prompts import CODE_SYSTEM_PROMPT, project_name_for_type
from app.services.html_generation_service import generate_html_project
from app.tools.docker_sandbox import (
    DOCKER_SANDBOX_ENV,
    build_docker_build_command,
    build_docker_install_command,
    run_docker_sandbox_checks,
)
from app.tools.html_sandbox import (
    BROWSER_SANDBOX_ENV,
    PREVIEW_CSP,
    build_preview_document,
    run_playwright_sandbox_checks,
    run_source_safety_checks,
    run_static_sandbox_checks,
)
from app.tools.project_repair import repair_package_json, repair_project_files
from app.tools.project_security import is_safe_project_path, validate_project_file_paths
from app.workflows.html_generation_workflow import inspect_html_generation_workflow

SECURITY_CONTENT_FIXTURE_PATH = (
    Path(__file__).resolve().parents[3] / "doc" / "security-content-fixtures.json"
)


def load_security_content_fixtures() -> list[dict]:
    return json.loads(SECURITY_CONTENT_FIXTURE_PATH.read_text(encoding="utf-8"))


def test_generate_html_project_returns_structured_files() -> None:
    project = generate_html_project(GenerateHtmlRequest(prompt="生成 Todo 应用"))

    assert project.project_name == "zerocode-html-app"
    assert [file.file_path for file in project.files] == [
        "index.html",
        "style.css",
        "script.js",
    ]


def test_generate_vue_project_returns_vite_skeleton() -> None:
    project = generate_html_project(
        GenerateHtmlRequest(prompt="生成仪表盘", projectType="vue"),
    )

    assert project.project_name == "zerocode-vue-app"
    assert [file.file_path for file in project.files] == [
        "package.json",
        "index.html",
        "vite.config.ts",
        "src/main.ts",
        "src/App.vue",
        "src/style.css",
    ]


def test_generate_react_project_returns_vite_skeleton() -> None:
    project = generate_html_project(
        GenerateHtmlRequest(prompt="生成仪表盘", projectType="react"),
    )

    assert project.project_name == "zerocode-react-app"
    assert [file.file_path for file in project.files] == [
        "package.json",
        "index.html",
        "vite.config.ts",
        "src/main.tsx",
        "src/App.tsx",
        "src/style.css",
    ]


def test_generation_updates_existing_html_project() -> None:
    base_project = generate_html_project(GenerateHtmlRequest(prompt="生成 Todo 应用"))

    project = generate_html_project(
        GenerateHtmlRequest(
            prompt="增加深色模式",
            projectType="html",
            baseProject=base_project,
        ),
    )
    html_file = next(file for file in project.files if file.file_path == "index.html")
    css_file = next(file for file in project.files if file.file_path == "style.css")

    assert project.project_name == "zerocode-html-app"
    assert '<section class="update-note">' in html_file.content
    assert "增加深色模式" in html_file.content
    assert ".update-note" in css_file.content
    assert [file.file_path for file in project.files] == [
        "index.html",
        "style.css",
        "script.js",
    ]


def test_generation_updates_existing_vue_project() -> None:
    base_project = generate_html_project(
        GenerateHtmlRequest(prompt="生成仪表盘", projectType="vue"),
    )

    project = generate_html_project(
        GenerateHtmlRequest(
            prompt="增加统计卡片",
            projectType="html",
            baseProject=base_project,
        ),
    )
    app_file = next(file for file in project.files if file.file_path == "src/App.vue")
    css_file = next(file for file in project.files if file.file_path == "src/style.css")

    assert project.project_type == "vue"
    assert '<section class="update-note">' in app_file.content
    assert "增加统计卡片" in app_file.content
    assert ".update-note" in css_file.content


def test_generation_updates_existing_react_project() -> None:
    base_project = generate_html_project(
        GenerateHtmlRequest(prompt="生成仪表盘", projectType="react"),
    )

    project = generate_html_project(
        GenerateHtmlRequest(
            prompt="增加趋势图",
            projectType="html",
            baseProject=base_project,
        ),
    )
    app_file = next(file for file in project.files if file.file_path == "src/App.tsx")

    assert project.project_type == "react"
    assert '<section className="update-note">' in app_file.content
    assert "增加趋势图" in app_file.content


def test_html_generation_uses_workflow_steps(monkeypatch) -> None:
    monkeypatch.delenv(BROWSER_SANDBOX_ENV, raising=False)

    result = inspect_html_generation_workflow("生成 Todo 应用")

    assert result["workflow_steps"] == ["planner", "ui", "code", "fix", "test"]
    assert result["issues"] == []
    assert result["test_report"] == [
        "File paths: safe",
        "HTML elements: 7",
        "CSS rules: present",
        "JavaScript: present",
        "Browser sandbox: skipped",
    ]


def test_project_generation_uses_skeleton_checks(monkeypatch) -> None:
    monkeypatch.delenv(DOCKER_SANDBOX_ENV, raising=False)

    result = inspect_html_generation_workflow("生成仪表盘", "vue")

    assert result["workflow_steps"] == ["planner", "ui", "code", "fix", "test"]
    assert result["issues"] == []
    assert result["test_report"] == [
        "File paths: safe",
        "package.json: present",
        "Vite plugin config: present",
        "module entry: present",
        "Vue entry import: present",
        "Vue app template: present",
        "Source safety: safe",
        "Docker sandbox: skipped",
    ]


def test_react_project_generation_checks_entry_connections(monkeypatch) -> None:
    monkeypatch.delenv(DOCKER_SANDBOX_ENV, raising=False)

    result = inspect_html_generation_workflow("生成仪表盘", "react")

    assert result["workflow_steps"] == ["planner", "ui", "code", "fix", "test"]
    assert result["issues"] == []
    assert result["test_report"] == [
        "File paths: safe",
        "package.json: present",
        "Vite plugin config: present",
        "module entry: present",
        "React entry import: present",
        "React app export: present",
        "Source safety: safe",
        "Docker sandbox: skipped",
    ]


def test_prompt_contracts_keep_structured_output_rules() -> None:
    assert "structured multi-file" in CODE_SYSTEM_PROMPT
    assert "never return Markdown code fences" in CODE_SYSTEM_PROMPT
    assert "sandbox-safe" in CODE_SYSTEM_PROMPT
    assert "do not reference external URLs" in CODE_SYSTEM_PROMPT
    assert "do not perform network requests" in CODE_SYSTEM_PROMPT
    assert "do not use inline event handlers" in CODE_SYSTEM_PROMPT
    assert "do not use eval or dynamic code execution" in CODE_SYSTEM_PROMPT
    assert project_name_for_type("react") == "zerocode-react-app"


def test_project_file_path_security_rejects_traversal_paths() -> None:
    assert is_safe_project_path("src/App.vue")
    assert not is_safe_project_path("../secret.txt")
    assert not is_safe_project_path("src/../secret.txt")
    assert not is_safe_project_path("/tmp/secret.txt")
    assert not is_safe_project_path("src//App.vue")

    result = validate_project_file_paths(
        [GeneratedFile(filePath="src/../secret.txt", fileType="txt", content="bad")],
    )

    assert result.issues == ["Unsafe file path: src/../secret.txt"]
    assert result.report == ["File paths: failed"]


def test_project_file_path_security_rejects_duplicate_normalized_paths() -> None:
    result = validate_project_file_paths(
        [
            GeneratedFile(filePath="src/App.vue", fileType="vue", content="one"),
            GeneratedFile(filePath=r"src\App.vue", fileType="vue", content="two"),
        ],
    )

    assert result.issues == ["Duplicate file path: src/App.vue"]
    assert result.report == ["File paths: failed"]


def test_repair_package_json_adds_vue_scripts_and_dependencies() -> None:
    content, report = repair_package_json('{"scripts":[],"dependencies":{}}', "vue")

    assert '"dev": "vite"' in content
    assert '"build": "vite build"' in content
    assert '"vue": "latest"' in content
    assert "package.json scripts: repaired" in report
    assert "package.json dependency vue: repaired" in report


def test_repair_package_json_recovers_invalid_json() -> None:
    content, report = repair_package_json("not-json", "react")

    assert '"react": "latest"' in content
    assert '"react-dom": "latest"' in content
    assert "package.json: replaced invalid JSON" in report


def test_repair_project_files_creates_missing_vue_entry_files() -> None:
    result = repair_project_files(
        [GeneratedFile(filePath="package.json", fileType="json", content="{}")],
        "vue",
    )

    paths = [file.file_path for file in result.files]
    assert paths == [
        "package.json",
        "index.html",
        "vite.config.ts",
        "src/main.ts",
        "src/App.vue",
        "src/style.css",
    ]
    assert "index.html: created" in result.report
    assert "vite.config.ts: created" in result.report
    assert "src/main.ts: created" in result.report
    assert "package.json dependency vue: repaired" in result.report


def test_repair_project_files_creates_missing_react_entry_files() -> None:
    result = repair_project_files([], "react")

    paths = [file.file_path for file in result.files]
    assert paths == [
        "package.json",
        "index.html",
        "vite.config.ts",
        "src/main.tsx",
        "src/App.tsx",
        "src/style.css",
    ]
    assert "package.json: created" in result.report
    assert "vite.config.ts: created" in result.report
    assert "src/main.tsx: created" in result.report
    assert "package.json dependency react-dom: repaired" in result.report


def test_repair_project_files_repairs_broken_vue_entry_files() -> None:
    result = repair_project_files(
        [
            GeneratedFile(filePath="package.json", fileType="json", content="{}"),
            GeneratedFile(filePath="index.html", fileType="html", content="<div></div>"),
            GeneratedFile(filePath="vite.config.ts", fileType="ts", content="export default {}"),
            GeneratedFile(filePath="src/main.ts", fileType="ts", content='import "./style.css"'),
            GeneratedFile(filePath="src/App.vue", fileType="vue", content="<script></script>"),
        ],
        "vue",
    )

    index_file = next(file for file in result.files if file.file_path == "index.html")
    config_file = next(file for file in result.files if file.file_path == "vite.config.ts")
    main_file = next(file for file in result.files if file.file_path == "src/main.ts")
    app_file = next(file for file in result.files if file.file_path == "src/App.vue")

    assert '<script type="module" src="/src/main.ts"></script>' in index_file.content
    assert "@vitejs/plugin-vue" in config_file.content
    assert 'import App from "./App.vue"' in main_file.content
    assert "<template>" in app_file.content
    assert "index.html: repaired" in result.report
    assert "vite.config.ts: repaired" in result.report
    assert "src/main.ts: repaired" in result.report
    assert "src/App.vue: repaired" in result.report


def test_repair_project_files_repairs_broken_react_entry_files() -> None:
    result = repair_project_files(
        [
            GeneratedFile(filePath="package.json", fileType="json", content="{}"),
            GeneratedFile(filePath="index.html", fileType="html", content="<div></div>"),
            GeneratedFile(filePath="vite.config.ts", fileType="ts", content="export default {}"),
            GeneratedFile(filePath="src/main.tsx", fileType="tsx", content='import "./style.css"'),
            GeneratedFile(
                filePath="src/App.tsx",
                fileType="tsx",
                content="export const Broken = 1",
            ),
        ],
        "react",
    )

    index_file = next(file for file in result.files if file.file_path == "index.html")
    config_file = next(file for file in result.files if file.file_path == "vite.config.ts")
    main_file = next(file for file in result.files if file.file_path == "src/main.tsx")
    app_file = next(file for file in result.files if file.file_path == "src/App.tsx")

    assert '<script type="module" src="/src/main.tsx"></script>' in index_file.content
    assert "@vitejs/plugin-react" in config_file.content
    assert 'import { App } from "./App"' in main_file.content
    assert "export function App" in app_file.content
    assert "index.html: repaired" in result.report
    assert "vite.config.ts: repaired" in result.report
    assert "src/main.tsx: repaired" in result.report
    assert "src/App.tsx: repaired" in result.report


def test_html_generation_escapes_prompt_content() -> None:
    project = generate_html_project(GenerateHtmlRequest(prompt='<script>alert("xss")</script>'))
    html_file = next(file for file in project.files if file.file_path == "index.html")

    assert "<script>alert" not in html_file.content
    assert "&lt;script&gt;" in html_file.content


def test_preview_document_combines_generated_files() -> None:
    project = generate_html_project(GenerateHtmlRequest(prompt="生成 Todo 应用"))

    document = build_preview_document(project.files)

    assert "<!doctype html>" in document
    assert '<html lang="zh-CN">' in document
    assert f'content="{PREVIEW_CSP}"' in document
    assert "connect-src 'none'" in document
    assert "<style>" in document
    assert "<script>" in document
    assert "primaryAction" in document


def test_preview_document_escapes_embedded_style_and_script_boundaries() -> None:
    document = build_preview_document(
        [
            GeneratedFile(filePath="index.html", fileType="html", content="<main>Ready</main>"),
            GeneratedFile(filePath="style.css", fileType="css", content="</style><img />"),
            GeneratedFile(filePath="script.js", fileType="js", content="</script><img />"),
        ],
    )

    assert "<\\/style><img />" in document
    assert "<\\/script><img />" in document
    assert "</style><img />" not in document
    assert "</script><img />" not in document


def test_static_sandbox_rejects_external_html_urls() -> None:
    result = run_static_sandbox_checks(
        [
            GeneratedFile(
                filePath="index.html",
                fileType="html",
                content='<main><img src="https://example.com/logo.png" /></main>',
            ),
            GeneratedFile(filePath="style.css", fileType="css", content="main { color: red; }"),
            GeneratedFile(filePath="script.js", fileType="js", content="console.log('ok')"),
        ],
    )

    assert "index.html references external URL: https://example.com/logo.png" in result.issues


def test_static_sandbox_rejects_unquoted_external_html_urls() -> None:
    result = run_static_sandbox_checks(
        [
            GeneratedFile(
                filePath="index.html",
                fileType="html",
                content="<main><img src=https://example.com/logo.png></main>",
            ),
            GeneratedFile(filePath="style.css", fileType="css", content="main { color: red; }"),
            GeneratedFile(filePath="script.js", fileType="js", content="console.log('ok')"),
        ],
    )

    assert "index.html references external URL: https://example.com/logo.png" in result.issues


def test_static_sandbox_allows_local_html_script_src() -> None:
    result = run_static_sandbox_checks(
        [
            GeneratedFile(
                filePath="index.html",
                fileType="html",
                content='<main>Ready</main><script type="module" src="/src/main.ts"></script>',
            ),
            GeneratedFile(filePath="style.css", fileType="css", content="main { color: red; }"),
            GeneratedFile(filePath="script.js", fileType="js", content="console.log('ok')"),
        ],
    )

    assert "index.html must not inline scripts; use script.js" not in result.issues


def test_static_sandbox_rejects_external_html_script_src() -> None:
    result = run_static_sandbox_checks(
        [
            GeneratedFile(
                filePath="index.html",
                fileType="html",
                content='<main>Ready</main><script src="https://example.com/app.js"></script>',
            ),
            GeneratedFile(filePath="style.css", fileType="css", content="main { color: red; }"),
            GeneratedFile(filePath="script.js", fileType="js", content="console.log('ok')"),
        ],
    )

    assert "index.html must not inline scripts; use script.js" not in result.issues
    assert "index.html references external URL: https://example.com/app.js" in result.issues


def test_static_sandbox_rejects_javascript_network_requests() -> None:
    result = run_static_sandbox_checks(
        [
            GeneratedFile(filePath="index.html", fileType="html", content="<main>Ready</main>"),
            GeneratedFile(filePath="style.css", fileType="css", content="main { color: red; }"),
            GeneratedFile(filePath="script.js", fileType="js", content="fetch('/api/data')"),
        ],
    )

    assert "script.js must not perform network requests" in result.issues


def test_static_sandbox_rejects_external_css_urls() -> None:
    result = run_static_sandbox_checks(
        [
            GeneratedFile(filePath="index.html", fileType="html", content="<main>Ready</main>"),
            GeneratedFile(
                filePath="style.css",
                fileType="css",
                content='main { background-image: url("https://example.com/bg.png"); }',
            ),
            GeneratedFile(filePath="script.js", fileType="js", content="console.log('ok')"),
        ],
    )

    assert "style.css must not reference external URLs" in result.issues


def test_static_sandbox_rejects_inline_event_handlers() -> None:
    result = run_static_sandbox_checks(
        [
            GeneratedFile(
                filePath="index.html",
                fileType="html",
                content='<main><button onclick="alert(1)">Run</button></main>',
            ),
            GeneratedFile(filePath="style.css", fileType="css", content="main { color: red; }"),
            GeneratedFile(filePath="script.js", fileType="js", content="console.log('ok')"),
        ],
    )

    assert "index.html must not use inline event handler: onclick" in result.issues


def test_static_sandbox_rejects_dynamic_code_execution() -> None:
    for script_content in [
        "eval('alert(1)')",
        "new Function('alert(1)')",
        "setTimeout('alert(1)', 100)",
    ]:
        result = run_static_sandbox_checks(
            [
                GeneratedFile(filePath="index.html", fileType="html", content="<main>Ready</main>"),
                GeneratedFile(filePath="style.css", fileType="css", content="main { color: red; }"),
                GeneratedFile(filePath="script.js", fileType="js", content=script_content),
            ],
        )

        assert "script.js must not use dynamic code execution" in result.issues


def test_static_sandbox_allows_regular_function_callbacks() -> None:
    result = run_static_sandbox_checks(
        [
            GeneratedFile(filePath="index.html", fileType="html", content="<main>Ready</main>"),
            GeneratedFile(filePath="style.css", fileType="css", content="main { color: red; }"),
            GeneratedFile(
                filePath="script.js",
                fileType="js",
                content="setTimeout(function () { console.log('ready') }, 100)",
            ),
        ],
    )

    assert "script.js must not use dynamic code execution" not in result.issues


def test_static_sandbox_matches_shared_content_security_fixtures() -> None:
    for fixture in load_security_content_fixtures():
        result = run_static_sandbox_checks(
            [
                GeneratedFile(
                    filePath=fixture["filePath"],
                    fileType=fixture["fileType"],
                    content=fixture["content"],
                ),
            ],
        )

        if fixture["allowed"]:
            assert result.issues == [], fixture["id"]
        else:
            assert fixture["expectedPythonIssue"] in result.issues, fixture["id"]


def test_docker_sandbox_skips_when_disabled(monkeypatch) -> None:
    monkeypatch.delenv(DOCKER_SANDBOX_ENV, raising=False)

    result = run_docker_sandbox_checks([], "vue")

    assert result.issues == []
    assert result.report == ["Docker sandbox: skipped"]


def test_docker_sandbox_is_not_required_for_html(monkeypatch) -> None:
    monkeypatch.setenv(DOCKER_SANDBOX_ENV, "true")

    result = run_docker_sandbox_checks([], "html")

    assert result.issues == []
    assert result.report == ["Docker sandbox: not required"]


def test_docker_sandbox_command_uses_isolation_options(tmp_path) -> None:
    install_command = build_docker_install_command(tmp_path)
    build_command = build_docker_build_command(tmp_path)

    assert install_command[:2] == ["docker", "run"]
    assert "--network" not in install_command
    assert "--read-only" in install_command
    assert "--tmpfs" in install_command
    assert f"{tmp_path}:/workspace:rw" in install_command
    assert install_command[-1] == (
        "npm_config_cache=/tmp/.npm npm install --ignore-scripts --no-audit --no-fund"
    )
    assert build_command[:2] == ["docker", "run"]
    assert "--network" in build_command
    assert "none" in build_command
    assert "--read-only" in build_command
    assert "--tmpfs" in build_command
    assert f"{tmp_path}:/workspace:rw" in build_command
    assert build_command[-1] == "npm run build"


def test_source_safety_checks_vue_and_react_project_files() -> None:
    result = run_source_safety_checks(
        [
            GeneratedFile(
                filePath="src/App.vue",
                fileType="vue",
                content="<script setup>fetch('/api/data')</script>",
            ),
            GeneratedFile(
                filePath="src/App.tsx",
                fileType="tsx",
                content="export function App() { eval('alert(1)'); return <main /> }",
            ),
            GeneratedFile(
                filePath="src/style.css",
                fileType="css",
                content='main { background-image: url("https://example.com/bg.png"); }',
            ),
        ],
    )

    assert result.issues == [
        "src/App.vue must not perform network requests",
        "src/App.tsx must not use dynamic code execution",
        "src/style.css must not reference external URLs",
    ]
    assert result.report == ["Source safety: failed"]


def test_browser_sandbox_is_opt_in_by_default(monkeypatch) -> None:
    monkeypatch.delenv(BROWSER_SANDBOX_ENV, raising=False)
    project = generate_html_project(GenerateHtmlRequest(prompt="生成 Todo 应用"))

    result = run_playwright_sandbox_checks(project.files)

    assert result.issues == []
    assert result.report == ["Browser sandbox: skipped"]


def test_browser_sandbox_enabled_renders_or_skips_for_environment(monkeypatch) -> None:
    monkeypatch.setenv(BROWSER_SANDBOX_ENV, "true")
    project = generate_html_project(GenerateHtmlRequest(prompt="生成 Todo 应用"))

    result = run_playwright_sandbox_checks(project.files)

    assert result.issues == []
    if result.report[0].startswith("Browser sandbox: skipped ("):
        return
    assert result.report[0] == "Browser sandbox: rendered"
    assert result.report[1].startswith("Browser sandbox text length: ")
    assert result.report[2].startswith("Browser sandbox visible elements: ")
    assert result.report[3].startswith("Browser sandbox screenshot bytes: ")


def test_browser_sandbox_reports_render_metrics(monkeypatch) -> None:
    monkeypatch.setenv(BROWSER_SANDBOX_ENV, "true")
    monkeypatch.setattr(html_sandbox, "sync_playwright", lambda: FakePlaywrightContext())
    project = generate_html_project(GenerateHtmlRequest(prompt="生成 Todo 应用"))

    result = run_playwright_sandbox_checks(project.files)

    assert result.issues == []
    assert result.report == [
        "Browser sandbox: rendered",
        "Browser sandbox text length: 8",
        "Browser sandbox visible elements: 3",
        "Browser sandbox screenshot bytes: 7",
    ]


class FakePlaywrightContext:
    def __enter__(self):
        return FakePlaywright()

    def __exit__(self, exc_type, exc, traceback):
        return False


class FakePlaywright:
    chromium = None

    def __init__(self) -> None:
        self.chromium = FakeChromium()


class FakeChromium:
    def launch(self, headless: bool):
        return FakeBrowser()


class FakeBrowser:
    def new_page(self, viewport: dict[str, int]):
        return FakePage()

    def close(self) -> None:
        return None


class FakePage:
    def set_content(self, content: str, wait_until: str) -> None:
        return None

    def locator(self, selector: str):
        if selector == "body":
            return FakeTextLocator()
        return FakeVisibleLocator()

    def screenshot(self, full_page: bool):
        return b"preview"


class FakeTextLocator:
    def inner_text(self, timeout: int):
        return "Rendered"


class FakeVisibleLocator:
    def count(self):
        return 3


def test_generate_html_api_returns_structured_response() -> None:
    client = TestClient(app)

    response = client.post("/generations/html", json={"prompt": "生成 Todo 应用"})

    assert response.status_code == 200
    payload = response.json()
    assert payload["code"] == 0
    assert payload["message"] == "ok"
    assert payload["data"]["projectName"] == "zerocode-html-app"


def test_generated_project_rejects_oversized_file_content() -> None:
    invalid_projects = [
        {
            "projectName": "x" * 129,
            "files": [{"filePath": "index.html", "fileType": "html", "content": "ok"}],
        },
        {
            "projectName": "zerocode-html-app",
            "files": [{"filePath": "x" * 501, "fileType": "html", "content": "ok"}],
        },
        {
            "projectName": "zerocode-html-app",
            "files": [{"filePath": "index.html", "fileType": "x" * 33, "content": "ok"}],
        },
        {
            "projectName": "zerocode-html-app",
            "files": [{"filePath": "index.html", "fileType": "html", "content": "x" * 200_001}],
        },
    ]

    for project in invalid_projects:
        try:
            GeneratedProject(projectType="html", **project)
        except ValidationError as error:
            assert error.errors()[0]["type"] == "string_too_long"
        else:
            raise AssertionError("Expected oversized generated project field to fail")


def test_oversized_base_project_uses_unified_response() -> None:
    client = TestClient(app)

    response = client.post(
        "/generations/html",
        json={
            "prompt": "更新项目",
            "baseProject": {
                "projectName": "zerocode-html-app",
                "projectType": "html",
                "files": [
                    {
                        "filePath": "index.html",
                        "fileType": "html",
                        "content": "x" * 200_001,
                    },
                ],
            },
        },
    )

    assert response.status_code == 400
    assert response.json() == {
        "code": 400,
        "data": None,
        "message": "Invalid request",
    }


def test_validation_error_uses_unified_response() -> None:
    client = TestClient(app)

    response = client.post("/generations/html", json={"prompt": ""})

    assert response.status_code == 400
    assert response.json() == {
        "code": 400,
        "data": None,
        "message": "Invalid request",
    }
