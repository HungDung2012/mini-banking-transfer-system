#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
LOG_DIR="$ROOT_DIR/logs"
PID_DIR="$ROOT_DIR/.run"

mkdir -p "$LOG_DIR" "$PID_DIR"

services=(
  "auth-service:services/auth-service"
  "account-service:services/account-service"
  "fraud-detection-service:services/fraud-detection-service"
  "transaction-service:services/transaction-service"
  "notification-service:services/notification-service"
  "audit-service:services/audit-service"
)

find_service_pids() {
  local module="$1"
  pgrep -f -- "-Dmaven.multiModuleProjectDirectory=$ROOT_DIR .* -pl $module spring-boot:run" || true
}

for entry in "${services[@]}"; do
  name="${entry%%:*}"
  module="${entry##*:}"
  log_file="$LOG_DIR/$name.log"
  pid_file="$PID_DIR/$name.pid"

  existing_pid="$(find_service_pids "$module" | head -n 1 || true)"
  if [[ -n "$existing_pid" ]]; then
    echo "$name is already running with pid $existing_pid"
    echo "$existing_pid" >"$pid_file"
    continue
  fi

  echo "Starting $name"
  nohup mvn -q -pl "$module" spring-boot:run >"$log_file" 2>&1 &
  echo $! >"$pid_file"
done

echo "Services started. Logs are available in $LOG_DIR"
