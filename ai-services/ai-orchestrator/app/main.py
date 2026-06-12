from contextlib import asynccontextmanager
from collections.abc import AsyncIterator

from fastapi import FastAPI

from app.config.exception_handlers import register_exception_handlers
from app.routers.generation_router import router as generation_router
from app.routers.health_router import router as health_router
from app.services.dependencies import get_llm_config


@asynccontextmanager
async def lifespan(_app: FastAPI) -> AsyncIterator[None]:
    # Validate LLM config at startup
    config = get_llm_config()
    if not config.api_key:
        from app.services.llm_client import NoOpLlmClient
        reason = "LLM_API_KEY not configured"
    else:
        reason = None
    if reason:
        import logging
        logging.getLogger(__name__).warning(
            "LLM client not available: %s. AI generation will use template fallback.", reason
        )
    yield


app = FastAPI(title="ZeroCode AI Orchestrator", version="0.1.0", lifespan=lifespan)
register_exception_handlers(app)

app.include_router(health_router)
app.include_router(generation_router, prefix="/generations", tags=["generations"])
