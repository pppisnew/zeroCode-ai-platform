from typing import Any


def ok(data: Any) -> dict[str, Any]:
    return {
        "code": 0,
        "data": data,
        "message": "ok",
    }


def fail(code: int, message: str) -> dict[str, Any]:
    return {
        "code": code,
        "data": None,
        "message": message,
    }
