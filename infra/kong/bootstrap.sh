#!/bin/sh
set -eu

KONG_ADMIN_URL="${KONG_ADMIN_URL:-http://kong:8001}"

wait_for_kong() {
  until curl -fsS "$KONG_ADMIN_URL/status" >/dev/null; do
    echo "Waiting for Kong Admin API..."
    sleep 2
  done
}

service_exists() {
  local name="$1"
  local status
  status="$(curl -s -o /dev/null -w "%{http_code}" "$KONG_ADMIN_URL/services/$name")"
  [ "$status" = "200" ]
}

route_exists() {
  local service_name="$1"
  local route_name="$2"
  local status
  status="$(curl -s -o /dev/null -w "%{http_code}" \
    "$KONG_ADMIN_URL/services/$service_name/routes/$route_name")"
  [ "$status" = "200" ]
}

jwt_plugin_exists() {
  local service_name="$1"
  curl -fsS "$KONG_ADMIN_URL/services/$service_name/plugins" | grep -q '"name":"jwt"'
}

create_service() {
  local name="$1"
  local url="$2"
  if service_exists "$name"; then
    echo "Kong service '$name' already exists."
    return
  fi

  curl -fsS -X POST "$KONG_ADMIN_URL/services" \
    --data "name=$name" \
    --data "url=$url" >/dev/null
}

create_route() {
  local service_name="$1"
  local route_name="$2"
  local path="$3"
  if route_exists "$service_name" "$route_name"; then
    echo "Kong route '$route_name' already exists."
    return
  fi

  curl -fsS -X POST "$KONG_ADMIN_URL/services/$service_name/routes" \
    --data "name=$route_name" \
    --data "paths[]=$path" \
    --data "strip_path=true" >/dev/null
}

ensure_jwt_plugin() {
  local service_name="$1"
  if jwt_plugin_exists "$service_name"; then
    echo "JWT plugin already attached to '$service_name'."
    return
  fi

  curl -fsS -X POST "$KONG_ADMIN_URL/services/$service_name/plugins" \
    --data "name=jwt" \
    --data "config.key_claim_name=sub" \
    --data "config.claims_to_verify[]=exp" \
    --data "config.header_names[]=authorization" >/dev/null
}

wait_for_kong

create_service "auth-service" "http://172.30.0.1:8081/auth"
create_route "auth-service" "auth-route" "/api/auth"

create_service "transaction-service" "http://172.30.0.1:8084/transfers"
create_route "transaction-service" "transfer-route" "/api/transfers"
ensure_jwt_plugin "transaction-service"

echo "Kong bootstrap completed."


