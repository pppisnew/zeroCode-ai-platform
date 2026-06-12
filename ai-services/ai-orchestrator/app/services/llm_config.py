from enum import Enum
from os import getenv

from pydantic import Field
from pydantic.dataclasses import dataclass


class LlmProvider(str, Enum):
    openai = "openai"
    anthropic = "anthropic"


@dataclass(frozen=True)
class LlmConfig:
    provider: LlmProvider = LlmProvider.openai
    api_key: str = ""
    model: str = "gpt-4o-mini"
    base_url: str | None = None
    max_tokens: int = 8192
    temperature: float = 0.3


def load_llm_config() -> LlmConfig:
    return LlmConfig(
        provider=LlmProvider(getenv("LLM_PROVIDER", "openai")),
        api_key=getenv("LLM_API_KEY", ""),
        model=getenv("LLM_MODEL", "gpt-4o-mini"),
        base_url=getenv("LLM_BASE_URL") or None,
        max_tokens=int(getenv("LLM_MAX_TOKENS", "8192")),
        temperature=float(getenv("LLM_TEMPERATURE", "0.3")),
    )
