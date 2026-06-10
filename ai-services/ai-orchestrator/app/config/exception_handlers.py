from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.models.api_response import fail


def register_exception_handlers(app: FastAPI) -> None:
    @app.exception_handler(RequestValidationError)
    async def handle_validation_error(
        request: Request, exception: RequestValidationError
    ) -> JSONResponse:
        return JSONResponse(status_code=400, content=fail(400, "Invalid request"))

    @app.exception_handler(ValueError)
    async def handle_value_error(request: Request, exception: ValueError) -> JSONResponse:
        return JSONResponse(status_code=400, content=fail(400, str(exception)))
