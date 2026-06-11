from dataclasses import dataclass

from app.models.generated_project import GeneratedFile


@dataclass(frozen=True)
class FilePathValidationResult:
    issues: list[str]
    report: list[str]


def validate_project_file_paths(files: list[GeneratedFile]) -> FilePathValidationResult:
    issues: list[str] = []
    seen_paths: set[str] = set()
    for file in files:
        normalized_path = normalize_project_path(file.file_path)
        if not is_safe_project_path(file.file_path):
            issues.append(f"Unsafe file path: {file.file_path}")
            continue
        if normalized_path in seen_paths:
            issues.append(f"Duplicate file path: {normalized_path}")
            continue
        seen_paths.add(normalized_path)

    if issues:
        return FilePathValidationResult(issues=issues, report=["File paths: failed"])
    return FilePathValidationResult(issues=[], report=["File paths: safe"])


def normalize_project_path(file_path: str) -> str:
    return file_path.replace("\\", "/")


def is_safe_project_path(file_path: str) -> bool:
    normalized = normalize_project_path(file_path).strip()
    if normalized.startswith("/") or not normalized:
        return False

    for segment in normalized.split("/"):
        if segment in {"", ".", ".."}:
            return False
    return True
