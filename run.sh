#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$ROOT_DIR/backend"
FRONTEND_DIR="$ROOT_DIR/frontend"

BACKEND_CMD="mvn"
if ! command -v mvn >/dev/null 2>&1; then
  BACKEND_CMD="./mvnw"
fi

cleanup() {
  if [[ -n "${BACKEND_PID:-}" ]] && kill -0 "$BACKEND_PID" 2>/dev/null; then
    kill "$BACKEND_PID" 2>/dev/null || true
  fi

  if [[ -n "${FRONTEND_PID:-}" ]] && kill -0 "$FRONTEND_PID" 2>/dev/null; then
    kill "$FRONTEND_PID" 2>/dev/null || true
  fi
}

trap cleanup EXIT INT TERM

if [[ ! -x "$FRONTEND_DIR/node_modules/.bin/next" ]]; then
  echo "Installing frontend dependencies"
  (cd "$FRONTEND_DIR" && npm install)
fi

echo "Starting backend on http://localhost:8080"
(cd "$BACKEND_DIR" && "$BACKEND_CMD" spring-boot:run) &
BACKEND_PID=$!

echo "Starting frontend on http://localhost:3000"
(cd "$FRONTEND_DIR" && npm run dev) &
FRONTEND_PID=$!

wait -n