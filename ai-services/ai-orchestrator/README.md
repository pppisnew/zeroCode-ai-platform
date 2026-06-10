# ZeroCode AI Orchestrator

Python AI service for prompt orchestration, agent workflows, code generation, fixing, and testing.

## Run

```bash
uv sync --python 3.12
uv run uvicorn app.main:app --reload --port 8000
```

If you prefer the standard library venv flow:

```bash
python3.12 -m venv .venv
.venv/bin/python -m pip install fastapi uvicorn pydantic langgraph pydantic-ai playwright
.venv/bin/python -m uvicorn app.main:app --reload --port 8000
```

## Health

```bash
curl http://localhost:8000/health
```
