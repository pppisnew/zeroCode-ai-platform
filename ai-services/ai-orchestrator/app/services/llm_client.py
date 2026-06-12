from __future__ import annotations

import json
from abc import ABC, abstractmethod
from typing import TypeVar

from openai import OpenAI
from pydantic import BaseModel

from app.services.llm_config import LlmConfig

T = TypeVar("T", bound=BaseModel)


class LlmClient(ABC):
    """Abstract LLM client. Implementations should handle provider-specific APIs."""

    @abstractmethod
    def chat(self, system_prompt: str, user_prompt: str) -> str: ...

    @abstractmethod
    def chat_structured(
        self,
        system_prompt: str,
        user_prompt: str,
        response_model: type[T],
    ) -> T: ...


class OpenAiLlmClient(LlmClient):
    def __init__(self, config: LlmConfig) -> None:
        kwargs: dict = {"api_key": config.api_key}
        if config.base_url:
            kwargs["base_url"] = config.base_url
        self._client = OpenAI(**kwargs)
        self._model = config.model
        self._max_tokens = config.max_tokens
        self._temperature = config.temperature

    def chat(self, system_prompt: str, user_prompt: str) -> str:
        response = self._client.chat.completions.create(
            model=self._model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            max_tokens=self._max_tokens,
            temperature=self._temperature,
        )
        return response.choices[0].message.content or ""

    def chat_structured(
        self,
        system_prompt: str,
        user_prompt: str,
        response_model: type[T],
    ) -> T:
        schema_name = response_model.__name__
        json_schema = _build_strict_schema(response_model, schema_name)

        response = self._client.chat.completions.create(
            model=self._model,
            messages=[
                {"role": "system", "content": system_prompt},
                {"role": "user", "content": user_prompt},
            ],
            max_tokens=self._max_tokens,
            temperature=self._temperature,
            response_format={
                "type": "json_schema",
                "json_schema": {
                    "name": schema_name,
                    "strict": True,
                    "schema": json_schema,
                },
            },
        )
        raw = response.choices[0].message.content or "{}"
        return response_model.model_validate_json(raw)


class NoOpLlmClient(LlmClient):
    """Returns empty/fallback responses. Used when no API key is configured."""

    def __init__(self, reason: str = "LLM_API_KEY not configured") -> None:
        self._reason = reason

    def chat(self, system_prompt: str, user_prompt: str) -> str:
        raise _no_api_key_error(self._reason)

    def chat_structured(
        self,
        system_prompt: str,
        user_prompt: str,
        response_model: type[T],
    ) -> T:
        raise _no_api_key_error(self._reason)


def _build_strict_schema(model: type[BaseModel], name: str) -> dict:
    raw = model.model_json_schema()
    return {
        "type": "object",
        "properties": raw.get("properties", {}),
        "required": raw.get("required", []),
        "additionalProperties": False,
    }


def _no_api_key_error(reason: str) -> RuntimeError:
    return RuntimeError(
        f"LLM client not available: {reason}. "
        "Set LLM_API_KEY in .env to enable AI generation."
    )
