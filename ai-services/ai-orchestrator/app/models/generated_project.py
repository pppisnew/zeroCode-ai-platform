from pydantic import BaseModel, Field

from app.models.project_limits import (
    MAX_FILE_CONTENT_LENGTH,
    MAX_FILE_PATH_LENGTH,
    MAX_FILE_TYPE_LENGTH,
    MAX_PROJECT_FILES,
    MAX_PROJECT_NAME_LENGTH,
)
from app.models.project_type import ProjectType


class GeneratedFile(BaseModel):
    file_path: str = Field(alias="filePath", min_length=1, max_length=MAX_FILE_PATH_LENGTH)
    file_type: str = Field(alias="fileType", min_length=1, max_length=MAX_FILE_TYPE_LENGTH)
    content: str = Field(max_length=MAX_FILE_CONTENT_LENGTH)


class GeneratedProject(BaseModel):
    project_name: str = Field(alias="projectName", min_length=1, max_length=MAX_PROJECT_NAME_LENGTH)
    project_type: ProjectType = Field(default="html", alias="projectType")
    files: list[GeneratedFile] = Field(min_length=1, max_length=MAX_PROJECT_FILES)
