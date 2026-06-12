from __future__ import annotations

import json
import logging
import re
from abc import ABC, abstractmethod
from typing import TypeVar

from openai import OpenAI
from pydantic import BaseModel

from app.services.llm_config import LlmConfig

logger = logging.getLogger(__name__)
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
        # Try strict json_schema first; fall back to json_object on failure
        self._use_strict_json_schema = True

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
        # Try strict json_schema first (OpenAI native)
        if self._use_strict_json_schema:
            try:
                return self._chat_with_strict_json_schema(
                    system_prompt, user_prompt, response_model
                )
            except Exception as exc:
                logger.warning(
                    "json_schema strict mode not supported by %s, falling back to json_object: %s",
                    self._model,
                    exc,
                )
                self._use_strict_json_schema = False

        # Fallback: json_object mode (DeepSeek, other OpenAI-compatible providers)
        return self._chat_with_json_object(system_prompt, user_prompt, response_model)

    def _chat_with_strict_json_schema(
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

    def _chat_with_json_object(
        self,
        system_prompt: str,
        user_prompt: str,
        response_model: type[T],
    ) -> T:
        schema_hint = _format_schema_hint(response_model)
        full_system = (
            f"{system_prompt}\n\n"
            "You MUST respond with a single valid JSON object. No markdown fences, "
            "no surrounding text, no explanation — ONLY the JSON object.\n"
            f"The JSON must match this schema:\n{schema_hint}"
        )
        # Try json_object format first; some providers reject it
        try:
            response = self._client.chat.completions.create(
                model=self._model,
                messages=[
                    {"role": "system", "content": full_system},
                    {"role": "user", "content": user_prompt},
                ],
                max_tokens=self._max_tokens,
                temperature=self._temperature,
                response_format={"type": "json_object"},
            )
        except Exception:
            # Final fallback: no response_format at all, rely on prompt instruction
            logger.info("json_object also unsupported, using plain text with JSON prompt instruction")
            response = self._client.chat.completions.create(
                model=self._model,
                messages=[
                    {"role": "system", "content": full_system},
                    {"role": "user", "content": user_prompt},
                ],
                max_tokens=self._max_tokens,
                temperature=self._temperature,
            )
        raw = _extract_json(response.choices[0].message.content or "{}")
        try:
            return response_model.model_validate_json(raw)
        except Exception as validation_error:
            logger.warning(
                "LLM JSON validation failed. Raw (first 500 chars): %s... Error: %s",
                raw[:500],
                validation_error,
            )
            # Retry once with the validation error fed back to LLM
            retry_system = (
                f"{full_system}\n\nThe previous response was invalid JSON. "
                f"Validation errors: {validation_error}. "
                "Please correct and return ONLY valid JSON matching the schema exactly."
            )
            retry_response = self._client.chat.completions.create(
                model=self._model,
                messages=[
                    {"role": "system", "content": retry_system},
                    {"role": "user", "content": user_prompt},
                    {"role": "assistant", "content": raw},
                ],
                max_tokens=self._max_tokens,
                temperature=self._temperature,
            )
            retry_raw = _extract_json(retry_response.choices[0].message.content or "{}")
            return response_model.model_validate_json(retry_raw)


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


def _extract_json(raw: str) -> str:
    """Strip markdown fences and extract the first JSON object from raw LLM output."""
    # Remove ```json ... ``` fences
    fence_match = re.search(r"```(?:json)?\s*(\{.*?\})\s*```", raw, re.DOTALL)
    if fence_match:
        return fence_match.group(1)
    # Find the outermost { ... }
    brace_match = re.search(r"\{.*\}", raw, re.DOTALL)
    if brace_match:
        return brace_match.group(0)
    return raw.strip()


def _format_schema_hint(model: type[BaseModel]) -> str:
    """Build a human-readable JSON schema hint using the model's JSON field names."""
    raw = model.model_json_schema()
    lines = ["{"]
    props = raw.get("properties", {})
    required = set(raw.get("required", []))

    # Build nested schema for GeneratedFile
    file_props = props.get("files", {}).get("items", {}).get("properties", {})
    file_required = props.get("files", {}).get("items", {}).get("required", [])

    lines.append(f'  "projectName": string (required),')
    lines.append(f'  "projectType": "html" | "vue" | "react",')
    lines.append('  "files": [')
    lines.append('    {')
    for j, (fkey, fprop) in enumerate(file_props.items()):
        comma = "," if j < len(file_props) - 1 else ""
        req_mark = " (required)" if fkey in file_required else ""
        ftype = fprop.get("type", "any")
        lines.append(f'      "{fkey}": {ftype}{req_mark}{comma}')
    lines.append('    }')
    lines.append('  ]')
    lines.append("}")
    return "\n".join(lines)


def _no_api_key_error(reason: str) -> RuntimeError:
    return RuntimeError(
        f"LLM client not available: {reason}. "
        "Set LLM_API_KEY in .env to enable AI generation."
    )
