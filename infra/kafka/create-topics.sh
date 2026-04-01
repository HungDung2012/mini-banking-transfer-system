#!/usr/bin/env bash
set -euo pipefail

BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-kafka:9092}"
TOPIC="${TOPIC:-transfer-events}"

until kafka-topics --bootstrap-server "${BOOTSTRAP_SERVER}" --list >/dev/null 2>&1; do
  echo "Waiting for Kafka at ${BOOTSTRAP_SERVER}..."
  sleep 2
done

kafka-topics \
  --bootstrap-server "${BOOTSTRAP_SERVER}" \
  --create \
  --if-not-exists \
  --topic "${TOPIC}" \
  --partitions 3 \
  --replication-factor 1