from __future__ import annotations

import subprocess
import tempfile
from dataclasses import dataclass
from os import getenv
from pathlib import Path

from app.models.generated_project import GeneratedFile
from app.models.project_type import ProjectType
from app.tools.project_security import is_safe_project_path, normalize_project_path

DOCKER_SANDBOX_ENV = "ZEROCODE_ENABLE_DOCKER_SANDBOX"
DOCKER_IMAGE_ENV = "ZEROCODE_DOCKER_SANDBOX_IMAGE"
DOCKER_TIMEOUT_SECONDS = 120
DEFAULT_DOCKER_IMAGE = "node:22-alpine"


@dataclass(frozen=True)
class DockerSandboxResult:
    issues: list[str]
    report: list[str]


def run_docker_sandbox_checks(
    files: list[GeneratedFile],
    project_type: ProjectType,
) -> DockerSandboxResult:
    if project_type == "html":
        return DockerSandboxResult(issues=[], report=["Docker sandbox: not required"])
    if getenv(DOCKER_SANDBOX_ENV) != "true":
        return DockerSandboxResult(issues=[], report=["Docker sandbox: skipped"])

    with tempfile.TemporaryDirectory(prefix="zerocode-sandbox-") as workspace:
        workspace_path = Path(workspace)
        try:
            write_project_files(workspace_path, files)
            install_result = subprocess.run(
                build_docker_install_command(workspace_path),
                capture_output=True,
                check=False,
                text=True,
                timeout=DOCKER_TIMEOUT_SECONDS,
            )
            if install_result.returncode != 0:
                return DockerSandboxResult(
                    issues=[],
                    report=[
                        "Docker sandbox: skipped",
                        install_result.stderr[-1_000:] or install_result.stdout[-1_000:],
                    ],
                )
            build_result = subprocess.run(
                build_docker_build_command(workspace_path),
                capture_output=True,
                check=False,
                text=True,
                timeout=DOCKER_TIMEOUT_SECONDS,
            )
        except (OSError, subprocess.SubprocessError) as error:
            return DockerSandboxResult(
                issues=[],
                report=["Docker sandbox: skipped", error.__class__.__name__],
            )

    if build_result.returncode != 0:
        return DockerSandboxResult(
            issues=["Docker sandbox build failed"],
            report=[
                "Docker sandbox: failed",
                build_result.stderr[-1_000:] or build_result.stdout[-1_000:],
            ],
        )
    return DockerSandboxResult(issues=[], report=["Docker sandbox: build passed"])


def build_docker_install_command(workspace_path: Path) -> list[str]:
    return [
        *build_docker_base_command(workspace_path),
        "sh",
        "-lc",
        "npm_config_cache=/tmp/.npm npm install --ignore-scripts --no-audit --no-fund",
    ]


def build_docker_build_command(workspace_path: Path) -> list[str]:
    return [
        *build_docker_base_command(workspace_path, network="none"),
        "sh",
        "-lc",
        "npm run build",
    ]


def build_docker_base_command(workspace_path: Path, network: str | None = None) -> list[str]:
    image = getenv(DOCKER_IMAGE_ENV, DEFAULT_DOCKER_IMAGE)
    command = [
        "docker",
        "run",
        "--rm",
        "--cpus",
        "1",
        "--memory",
        "512m",
        "--pids-limit",
        "128",
        "--read-only",
        "--tmpfs",
        "/tmp:rw,noexec,nosuid,size=128m",
        "-v",
        f"{workspace_path}:/workspace:rw",
        "-w",
        "/workspace",
        image,
    ]
    if network is not None:
        command[3:3] = ["--network", network]
    return command


def write_project_files(workspace_path: Path, files: list[GeneratedFile]) -> None:
    for file in files:
        if not is_safe_project_path(file.file_path):
            raise ValueError("Invalid file path")
        target_path = workspace_path / normalize_project_path(file.file_path)
        target_path.parent.mkdir(parents=True, exist_ok=True)
        target_path.write_text(file.content, encoding="utf-8")
