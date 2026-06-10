from fastapi import FastAPI

from app.config.exception_handlers import register_exception_handlers
from app.routers.generation_router import router as generation_router
from app.routers.health_router import router as health_router

app = FastAPI(title="ZeroCode AI Orchestrator", version="0.1.0")
register_exception_handlers(app)

app.include_router(health_router)
app.include_router(generation_router, prefix="/generations", tags=["generations"])
