# ZeroCode AI Platform

AI-powered low-code/no-code Web application generation platform.

## Structure

```text
frontend/                    Vue 3 + TypeScript + Vite UI
backend/platform-service/    Java 21 + Spring Boot 3 platform layer
ai-services/ai-orchestrator/ Python 3.12+ + FastAPI AI layer
infra/                       MySQL, Redis, RabbitMQ, MinIO
doc/                         Product and architecture documents
```

## Operations Documents

- `doc/operations.md`: startup, configuration, verification, troubleshooting, and maintenance runbook.
- `doc/deployment.md`: deployment package and deploy-service executor boundaries.
- `doc/security-rules.md`: frontend/Python/Java security rule alignment.
- `doc/task-current.md`: current task state and recovery context.
- `doc/git.md`: Git remote, ignore rules, and commit workflow.

## Run Infrastructure

```bash
cd infra
docker compose up -d
```

## Run Frontend

```bash
cd frontend
npm run dev
```

## Run Java Platform Service

```bash
cd backend/platform-service
mvn spring-boot:run
```

Health endpoint:

```bash
curl http://localhost:8080/api/health
```

## Run Python AI Orchestrator

```bash
cd ai-services/ai-orchestrator
uv sync --python 3.12
uv run uvicorn app.main:app --reload --port 8000
```

The AI service requires Python 3.12+. A `.python-version` file is included for local toolchains that respect it.

Health endpoint:

```bash
curl http://localhost:8000/health
```
