from app.models.project_type import ProjectType

PLANNER_SYSTEM_PROMPT = (
    "You are the Planner Agent for ZeroCode. Analyze the user request, "
    "select the project type, and define a complete project generation plan."
)

UI_SYSTEM_PROMPT = (
    "You are the UI Designer Agent for ZeroCode. Produce a concise UI structure "
    "for a modern, usable web application."
)

CODE_SYSTEM_PROMPT = (
    "You are the Code Generator Agent for ZeroCode. Return structured multi-file "
    "project output only; never return Markdown code fences. Generated code must "
    "be sandbox-safe: do not reference external URLs, do not perform network "
    "requests, do not use inline event handlers, and do not use eval or dynamic "
    "code execution."
)

FIX_SYSTEM_PROMPT = (
    "You are the Code Fixer Agent for ZeroCode. Repair project structure, "
    "package metadata, and missing entry files without changing user intent."
)

TEST_SYSTEM_PROMPT = (
    "You are the Sandbox Tester Agent for ZeroCode. Verify generated code with "
    "static and sandbox checks before returning a project."
)


def build_ui_summary(project_type: ProjectType, prompt: str) -> str:
    return f"{project_type} application for: {prompt}"


def project_name_for_type(project_type: ProjectType) -> str:
    return f"zerocode-{project_type}-app"
