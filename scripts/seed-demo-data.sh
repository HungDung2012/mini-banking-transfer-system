#!/usr/bin/env bash
set -euo pipefail

AUTH_BASE_URL="${AUTH_BASE_URL:-http://localhost:8000}"
ACCOUNT_BASE_URL="${ACCOUNT_BASE_URL:-http://localhost:8082}"

echo "Seeding auth data via: $AUTH_BASE_URL"
echo "Seeding account data via: $ACCOUNT_BASE_URL"

post_json() {
  local url="$1"
  local body="$2"

  local status
  status="$(curl -sS -o /tmp/seed-response.txt -w "%{http_code}" \
    -X POST "$url" \
    -H "Content-Type: application/json" \
    -d "$body")"

  if [[ "$status" != "200" && "$status" != "201" && "$status" != "409" ]]; then
    cat /tmp/seed-response.txt
    echo
    echo "Seed request failed with HTTP $status for $url" >&2
    exit 1
  fi
}

post_json "$AUTH_BASE_URL/api/auth/register" '{"username":"alice","password":"secret123"}'
post_json "$AUTH_BASE_URL/api/auth/register" '{"username":"bob","password":"secret123"}'
post_json "$ACCOUNT_BASE_URL/accounts" '{"accountNumber":"100001","ownerName":"Alice","balance":1000000000}'
post_json "$ACCOUNT_BASE_URL/accounts" '{"accountNumber":"200001","ownerName":"Bob","balance":500000}'

echo "Demo data seeded."
