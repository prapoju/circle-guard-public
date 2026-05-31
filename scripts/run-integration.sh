#!/bin/bash
set -e

SERVICES=("auth" "identity" "promotion" "notification" "form" "file" "gateway" "dashboard")

for svc in "${SERVICES[@]}"; do
  echo "=== Integration tests: circleguard-$svc-service ==="
  ./gradlew ":services:circleguard-$svc-service:integrationTest"
done

echo "=== All integration tests passed ==="
