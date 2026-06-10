from app.models.generated_project import GeneratedProject
from app.models.generation_request import GenerateHtmlRequest
from app.workflows.html_generation_workflow import run_html_generation_workflow


def generate_html_project(request: GenerateHtmlRequest) -> GeneratedProject:
    return run_html_generation_workflow(request.prompt, request.project_type, request.base_project)
