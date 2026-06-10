from datetime import UTC, datetime

from fastapi import APIRouter

from app.models.api_response import ok

router = APIRouter(tags=["health"])


@router.get("/health")
def health() -> dict[str, object]:
    return ok(
        {
            "service": "ai-orchestrator",
            "status": "up",
            "timestamp": datetime.now(UTC).isoformat(),
        }
    )
