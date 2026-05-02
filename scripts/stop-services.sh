#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
PID_DIR="$ROOT_DIR/.run"

services=(
  "auth-service:services/auth-service"
  "account-service:services/account-service"
  "fraud-detection-service:services/fraud-detection-service"
  "transaction-service:services/transaction-service"
  "notification-service:services/notification-service"
  "audit-service:services/audit-service"
)

ports=(8081 8082 8083 8084 8085 8086)

if [[ ! -d "$PID_DIR" ]]; then
  echo "No pid directory found."
  mkdir -p "$PID_DIR"
fi

find_service_pids() {
  local module="$1"
  pgrep -f -- "-f $ROOT_DIR/$module/pom.xml .* spring-boot:run" || true
}

for pid_file in "$PID_DIR"/*.pid; do
  [[ -e "$pid_file" ]] || continue
  name="$(basename "$pid_file" .pid)"
  pid="$(cat "$pid_file")"

  if kill -0 "$pid" 2>/dev/null; then
    echo "Stopping $name ($pid)"
    kill "$pid"
  else
    echo "$name is not running"
  fi

  rm -f "$pid_file"
done

for entry in "${services[@]}"; do
  name="${entry%%:*}"
  module="${entry##*:}"
  matching_pids="$(find_service_pids "$module")"

  if [[ -z "$matching_pids" ]]; then
    continue
  fi

  while IFS= read -r pid; do
    [[ -n "$pid" ]] || continue
    echo "Stopping lingering $name process ($pid)"
    kill "$pid" 2>/dev/null || true
  done <<< "$matching_pids"
done

sleep 2

for entry in "${services[@]}"; do
  module="${entry##*:}"
  lingering_pids="$(find_service_pids "$module")"
  if [[ -n "$lingering_pids" ]]; then
    while IFS= read -r pid; do
      [[ -n "$pid" ]] || continue
      kill -9 "$pid" 2>/dev/null || true
    done <<< "$lingering_pids"
  fi
done

for port in "${ports[@]}"; do
  port_pids="$(sudo lsof -ti tcp:"$port" 2>/dev/null || true)"
  if [[ -n "$port_pids" ]]; then
    while IFS= read -r pid; do
      [[ -n "$pid" ]] || continue
      echo "Stopping process on port $port ($pid)"
      sudo kill -9 "$pid" 2>/dev/null || true
    done <<< "$port_pids"
  fi
done
