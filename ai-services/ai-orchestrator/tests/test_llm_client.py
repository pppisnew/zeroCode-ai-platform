import json
from unittest.mock import MagicMock, patch

import pytest
from openai.types.chat import ChatCompletion, ChatCompletionMessage
from openai.types.chat.chat_completion import Choice

from app.services.llm_client import NoOpLlmClient, OpenAiLlmClient
from app.services.llm_config import LlmConfig, LlmProvider


def make_mock_completion(content: str) -> ChatCompletion:
    return ChatCompletion(
        id="fake-id",
        choices=[
            Choice(
                finish_reason="stop",
                index=0,
                message=ChatCompletionMessage(content=content, role="assistant"),
            )
        ],
        created=1234567890,
        model="gpt-4o-mini",
        object="chat.completion",
    )


class TestOpenAiLlmClient:
    def test_chat_returns_content(self):
        config = LlmConfig(
            provider=LlmProvider.openai,
            api_key="sk-test",
            model="gpt-4o-mini",
        )
        client = OpenAiLlmClient(config)
        mock_response = make_mock_completion("Hello, world!")

        with patch.object(client._client.chat.completions, "create", return_value=mock_response):
            result = client.chat("System prompt", "User prompt")

        assert result == "Hello, world!"

    def test_chat_structured_returns_parsed_model(self):
        from pydantic import BaseModel

        class TestOutput(BaseModel):
            name: str
            count: int

        config = LlmConfig(
            provider=LlmProvider.openai,
            api_key="sk-test",
            model="gpt-4o-mini",
        )
        client = OpenAiLlmClient(config)
        mock_response = make_mock_completion('{"name": "test", "count": 42}')

        with patch.object(client._client.chat.completions, "create", return_value=mock_response):
            result = client.chat_structured("System", "User", TestOutput)

        assert result.name == "test"
        assert result.count == 42

    def test_chat_handles_empty_content(self):
        config = LlmConfig(provider=LlmProvider.openai, api_key="sk-test")
        client = OpenAiLlmClient(config)
        mock_response = make_mock_completion("")

        with patch.object(client._client.chat.completions, "create", return_value=mock_response):
            result = client.chat("System", "User")

        assert result == ""


class TestNoOpLlmClient:
    def test_chat_raises_runtime_error(self):
        client = NoOpLlmClient("no API key")
        with pytest.raises(RuntimeError, match="no API key"):
            client.chat("System", "User")

    def test_chat_structured_raises_runtime_error(self):
        from pydantic import BaseModel

        class TestOutput(BaseModel):
            name: str

        client = NoOpLlmClient("test reason")
        with pytest.raises(RuntimeError, match="test reason"):
            client.chat_structured("System", "User", TestOutput)
