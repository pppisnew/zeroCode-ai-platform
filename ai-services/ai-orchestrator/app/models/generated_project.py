from pydantic import BaseModel, Field

from app.models.project_type import ProjectType


class GeneratedFile(BaseModel):
    file_path: str = Field(alias="filePath", min_length=1, max_length=500)
    file_type: str = Field(alias="fileType", min_length=1, max_length=32)
    content: str = Field(max_length=200_000)


class GeneratedProject(BaseModel):
    project_name: str = Field(alias="projectName", min_length=1, max_length=128)
    project_type: ProjectType = Field(default="html", alias="projectType")
    files: list[GeneratedFile] = Field(min_length=1, max_length=100)
