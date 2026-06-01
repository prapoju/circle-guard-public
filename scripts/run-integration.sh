#!/bin/bash
set -e

SERVICES=("auth" "identity" "promotion" "notification" "form" "file" "gateway" "dashboard")

for svc in "${SERVICES[@]}"; do
  compose_file="services/circleguard-$svc-service/docker-compose.integration.yml"

  echo "=== Integration tests: circleguard-$svc-service ==="

  if [ -f "$compose_file" ]; then
    docker compose -p circleguard -f "$compose_file" down -v 2>/dev/null || true
    docker compose -p circleguard -f "$compose_file" up -d --wait
  fi

  ./gradlew ":services:circleguard-$svc-service:integrationTest"
done

echo "=== All integration tests passed ==="
