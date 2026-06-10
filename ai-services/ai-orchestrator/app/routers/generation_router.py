from fastapi import APIRouter

from app.models.api_response import ok
from app.models.generated_project import GeneratedProject
from app.models.generation_request import GenerateHtmlRequest
from app.services.html_generation_service import generate_html_project

router = APIRouter()


@router.post("/html")
def generate_html(request: GenerateHtmlRequest) -> dict[str, object]:
    project: GeneratedProject = generate_html_project(request)
    return ok(project.model_dump(by_alias=True))
