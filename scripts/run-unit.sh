#!/bin/bash
set -e

SERVICES=("auth" "identity" "promotion" "notification" "form" "file" "gateway" "dashboard")

for svc in "${SERVICES[@]}"; do
  echo "=== Unit tests: circleguard-$svc-service ==="
  ./gradlew ":services:circleguard-$svc-service:test"
done

echo "=== All unit tests passed ==="
