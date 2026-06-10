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
- `doc/project-learning-guide.md`: beginner-friendly project walkthrough and code reading guide.
- `doc/deployment.md`: deployment package and deploy-service executor boundaries.
- `doc/security-rules.md`: frontend/Python/Java security rule alignment.
- `doc/task-current.md`: current task state and recovery context.
- `doc/git.md`: Git remote, ignore rules, and commit workflow.

## Run Infrastructure

Recommended full local startup:

```bash
cp .env.example .env
scripts/start-project.sh
```

The default local MySQL host port is `3307` to avoid conflicts with a MySQL
server already running on `127.0.0.1:3306`.

Stop application services:

```bash
scripts/stop-project.sh
```

Stop application services and infrastructure:

```bash
scripts/stop-project.sh --infra
```

Manual infrastructure startup:

```bash
cd infra
docker compose --env-file ../.env up -d
```

## Local Integration Check

After `scripts/start-project.sh` finishes, verify the real frontend-to-backend
path through the Vite proxy:

```bash
curl http://localhost:5173/api/health
curl http://localhost:5173/api/apps
curl -X POST http://localhost:5173/api/generations/html \
  -H 'Content-Type: application/json' \
  --data '{"prompt":"生成一个个人作品集首页，包含项目列表和联系按钮","projectType":"html"}'
```

Expected result:

- `/api/health` returns `code: 0`.
- `/api/apps` returns `code: 0`.
- `/api/generations/html` returns `code: 0` and creates one app version in MySQL.

If the frontend shows `Request failed`, first check whether Vite and Platform
Service are listening on `5173` and `8080`. If generation returns `Internal
server error`, inspect `.runtime/logs/platform-service.log`; a common local
cause is `MYSQL_PORT` pointing at a host MySQL instead of the Docker MySQL.

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
