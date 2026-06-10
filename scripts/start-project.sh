#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/.runtime"
PID_DIR="$RUNTIME_DIR/pids"
LOG_DIR="$RUNTIME_DIR/logs"
ENV_FILE="$ROOT_DIR/.env"

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

if [[ -f "$ENV_FILE" ]]; then
  echo "Loading local configuration from $ENV_FILE"
  set -a
  # shellcheck disable=SC1090
  source "$ENV_FILE"
  set +a
fi

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

is_port_in_use() {
  local port="$1"
  command -v lsof >/dev/null 2>&1 && lsof -nP -iTCP:"$port" -sTCP:LISTEN >/dev/null 2>&1
}

print_port_owner() {
  local port="$1"
  if command -v lsof >/dev/null 2>&1; then
    lsof -nP -iTCP:"$port" -sTCP:LISTEN >&2 || true
  fi
}

start_service() {
  local name="$1"
  local workdir="$2"
  local port="$3"
  shift 3

  local pid_file="$PID_DIR/$name.pid"
  local log_file="$LOG_DIR/$name.log"

  if is_running "$pid_file"; then
    echo "$name is already running with PID $(cat "$pid_file")"
    return
  fi

  if [[ "$port" != "-" ]] && is_port_in_use "$port"; then
    echo "$name cannot start because port $port is already in use." >&2
    print_port_owner "$port"
    exit 1
  fi

  echo "Starting $name..."
  (
    cd "$workdir"
    nohup "$@" >"$log_file" 2>&1 &
    echo $! >"$pid_file"
  )
  sleep 3
  if ! is_running "$pid_file"; then
    echo "$name failed to stay running; log: $log_file" >&2
    tail -80 "$log_file" >&2 || true
    exit 1
  fi
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
  8000 \
  env UV_CACHE_DIR="${UV_CACHE_DIR:-/private/tmp/uv-cache}" uv run python -m uvicorn app.main:app --reload --port 8000

start_service \
  deploy-service \
  "$ROOT_DIR/backend/deploy-service" \
  8081 \
  mvn spring-boot:run

start_service \
  platform-service \
  "$ROOT_DIR/backend/platform-service" \
  8080 \
  mvn spring-boot:run

start_service \
  frontend \
  "$ROOT_DIR/frontend" \
  5173 \
  npm run dev -- --strictPort

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
