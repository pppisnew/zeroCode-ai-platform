from langgraph.graph import END, StateGraph

from app.agents.html_generation_agents import (
    HtmlGenerationState,
    code_node,
    fix_node,
    planner_node,
    test_node,
    ui_node,
)
from app.models.generated_project import GeneratedProject
from app.models.project_type import ProjectType


def _build_workflow():
    workflow = StateGraph(HtmlGenerationState)
    workflow.add_node("planner", planner_node)
    workflow.add_node("ui", ui_node)
    workflow.add_node("code", code_node)
    workflow.add_node("fix", fix_node)
    workflow.add_node("test", test_node)

    workflow.set_entry_point("planner")
    workflow.add_edge("planner", "ui")
    workflow.add_edge("ui", "code")
    workflow.add_edge("code", "fix")
    workflow.add_edge("fix", "test")
    workflow.add_edge("test", END)
    return workflow.compile()


html_generation_workflow = _build_workflow()


def run_html_generation_workflow(
    prompt: str,
    project_type: ProjectType = "html",
    base_project: GeneratedProject | None = None,
) -> GeneratedProject:
    result = html_generation_workflow.invoke(
        {
            "prompt": prompt,
            "project_type": project_type,
            "base_project": base_project,
        }
    )
    return GeneratedProject(
        projectName=result["project_name"],
        projectType=result["project_type"],
        files=result["files"],
    )


def inspect_html_generation_workflow(
    prompt: str,
    project_type: ProjectType = "html",
) -> HtmlGenerationState:
    return html_generation_workflow.invoke({"prompt": prompt, "project_type": project_type})
