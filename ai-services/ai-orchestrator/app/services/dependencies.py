from functools import lru_cache

from app.services.llm_client import LlmClient, NoOpLlmClient, OpenAiLlmClient
from app.services.llm_config import LlmConfig, LlmProvider, load_llm_config


@lru_cache
def get_llm_config() -> LlmConfig:
    return load_llm_config()


@lru_cache
def get_llm_client() -> LlmClient:
    config = get_llm_config()
    if not config.api_key:
        return NoOpLlmClient("LLM_API_KEY not configured")
    if config.provider == LlmProvider.openai:
        return OpenAiLlmClient(config)
    return NoOpLlmClient(f"unsupported LLM provider: {config.provider.value}")
