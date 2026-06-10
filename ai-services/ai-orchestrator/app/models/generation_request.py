from pydantic import BaseModel, Field

from app.models.generated_project import GeneratedProject
from app.models.project_type import ProjectType


class GenerateHtmlRequest(BaseModel):
    prompt: str = Field(min_length=1, max_length=4000)
    app_id: int | None = Field(default=None, alias="appId")
    project_type: ProjectType = Field(default="html", alias="projectType")
    base_project: GeneratedProject | None = Field(default=None, alias="baseProject")
