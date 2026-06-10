#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
RUNTIME_DIR="$ROOT_DIR/.runtime"
PID_DIR="$RUNTIME_DIR/pids"
LOG_DIR="$RUNTIME_DIR/logs"

STOP_INFRA=false
REMOVE_VOLUMES=false

usage() {
  cat <<'USAGE'
Usage: scripts/stop-project.sh [options]

Options:
  --infra       Also stop docker compose infra services.
  --volumes     Stop infra and remove docker compose volumes. This deletes local data.
  -h, --help    Show this help message.

By default this script stops only application services started by start-project.sh:
  frontend, platform-service, deploy-service, ai-orchestrator.
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --infra)
      STOP_INFRA=true
      shift
      ;;
    --volumes)
      STOP_INFRA=true
      REMOVE_VOLUMES=true
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

stop_service() {
  local name="$1"
  local pid_file="$PID_DIR/$name.pid"

  if [[ ! -f "$pid_file" ]]; then
    echo "$name is not tracked; no PID file."
    return
  fi

  local pid
  pid="$(cat "$pid_file")"

  if ! kill -0 "$pid" >/dev/null 2>&1; then
    echo "$name is not running; removing stale PID file."
    rm -f "$pid_file"
    return
  fi

  echo "Stopping $name with PID $pid..."
  pkill -TERM -P "$pid" >/dev/null 2>&1 || true
  kill "$pid"

  for _ in {1..20}; do
    if ! kill -0 "$pid" >/dev/null 2>&1; then
      rm -f "$pid_file"
      echo "$name stopped."
      return
    fi
    sleep 1
  done

  echo "$name did not stop in time; forcing shutdown..."
  pkill -KILL -P "$pid" >/dev/null 2>&1 || true
  kill -9 "$pid" >/dev/null 2>&1 || true
  rm -f "$pid_file"
  echo "$name stopped."
}

stop_service frontend
stop_service platform-service
stop_service deploy-service
stop_service ai-orchestrator

if [[ "$STOP_INFRA" == true ]]; then
  if ! command -v docker >/dev/null 2>&1; then
    echo "Missing required command: docker" >&2
    exit 1
  fi

  echo "Stopping infra services..."
  if [[ "$REMOVE_VOLUMES" == true ]]; then
    (
      cd "$ROOT_DIR/infra"
      docker compose down -v
    )
  else
    (
      cd "$ROOT_DIR/infra"
      docker compose down
    )
  fi
fi

cat <<EOF

Project shutdown requested.

Logs remain available at:
  $LOG_DIR
EOF
