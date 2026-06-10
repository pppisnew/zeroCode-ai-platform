#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/.runtime"
PID_DIR="$RUNTIME_DIR/pids"
LOG_DIR="$RUNTIME_DIR/logs"

START_INFRA=true
AUTO_INSTALL=true

usage() {
  cat <<'USAGE'
Usage: scripts/start-project.sh [options]

Options:
  --skip-infra       Do not run docker compose up -d for infra services.
  --no-install       Do not auto-install missing frontend/Python dependencies.
  -h, --help         Show this help message.

Services started:
  infra              MySQL, Redis, RabbitMQ, MinIO by docker compose.
  ai-orchestrator    FastAPI on http://localhost:8000.
  deploy-service     Spring Boot on http://localhost:8081.
  platform-service   Spring Boot on http://localhost:8080/api.
  frontend           Vite dev server on http://localhost:5173.

Runtime files:
  .runtime/pids/*.pid
  .runtime/logs/*.log
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --skip-infra)
      START_INFRA=false
      shift
      ;;
    --no-install)
      AUTO_INSTALL=false
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      usage
      exit 1
      ;;
  esac
done

mkdir -p "$PID_DIR" "$LOG_DIR"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "Missing required command: $1" >&2
    exit 1
  fi
}

is_running() {
  local pid_file="$1"
  [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" >/dev/null 2>&1
}

start_service() {
  local name="$1"
  local workdir="$2"
  shift 2

  local pid_file="$PID_DIR/$name.pid"
  local log_file="$LOG_DIR/$name.log"

  if is_running "$pid_file"; then
    echo "$name is already running with PID $(cat "$pid_file")"
    return
  fi

  echo "Starting $name..."
  (
    cd "$workdir"
    nohup "$@" >"$log_file" 2>&1 &
    echo $! >"$pid_file"
  )
  echo "$name started with PID $(cat "$pid_file"); log: $log_file"
}

if [[ "$START_INFRA" == true ]]; then
  require_command docker
  echo "Starting infra services..."
  (
    cd "$ROOT_DIR/infra"
    docker compose up -d
  )
fi

require_command uv
require_command mvn
require_command npm

if [[ "$AUTO_INSTALL" == true && ! -d "$ROOT_DIR/ai-services/ai-orchestrator/.venv" ]]; then
  echo "Python virtual environment is missing; running uv sync..."
  (
    cd "$ROOT_DIR/ai-services/ai-orchestrator"
    UV_CACHE_DIR="${UV_CACHE_DIR:-/private/tmp/uv-cache}" uv sync
  )
fi

if [[ "$AUTO_INSTALL" == true && ! -d "$ROOT_DIR/frontend/node_modules" ]]; then
  echo "Frontend node_modules is missing; running npm install..."
  (
    cd "$ROOT_DIR/frontend"
    npm install
  )
fi

start_service \
  ai-orchestrator \
  "$ROOT_DIR/ai-services/ai-orchestrator" \
  env UV_CACHE_DIR="${UV_CACHE_DIR:-/private/tmp/uv-cache}" uv run uvicorn app.main:app --reload --port 8000

start_service \
  deploy-service \
  "$ROOT_DIR/backend/deploy-service" \
  mvn spring-boot:run

start_service \
  platform-service \
  "$ROOT_DIR/backend/platform-service" \
  mvn spring-boot:run

start_service \
  frontend \
  "$ROOT_DIR/frontend" \
  npm run dev

cat <<EOF

Project startup requested.

Useful URLs:
  Frontend:          http://localhost:5173
  AI health:         http://localhost:8000/health
  Platform health:   http://localhost:8080/api/health
  Deploy service:    http://localhost:8081/deployments/not-found

Logs:
  $LOG_DIR

Stop:
  scripts/stop-project.sh
  scripts/stop-project.sh --infra
EOF
