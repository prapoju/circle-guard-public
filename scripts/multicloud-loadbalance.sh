#!/bin/bash
# Cross-cloud load balancing between the Azure (AKS) and DigitalOcean (DOKS)
# gateways.
#
# This is the local fallback for the DNS-based balancer (Cloudflare weighted
# records + health checks). It health-checks both ingress endpoints and
# round-robins traffic across the healthy ones, doing automatic failover when
# one cloud is down. No credentials are stored here.
#
# Usage:
#   AZURE_GATEWAY=https://azure.example.com \
#   DO_GATEWAY=https://do.example.com \
#   ./scripts/multicloud-loadbalance.sh [-n requests] [-p path]
#
#   -n  number of requests to dispatch (default 10)
#   -p  request path (default /actuator/health)
set -euo pipefail

REQUESTS=10
PATH_="/actuator/health"
HEALTH_PATH="/actuator/health"

while getopts "n:p:" opt; do
  case "$opt" in
    n) REQUESTS="$OPTARG" ;;
    p) PATH_="$OPTARG" ;;
    *) echo "usage: $0 [-n requests] [-p path]" >&2; exit 2 ;;
  esac
done

: "${AZURE_GATEWAY:?set AZURE_GATEWAY to the Azure ingress URL}"
: "${DO_GATEWAY:?set DO_GATEWAY to the DigitalOcean ingress URL}"

declare -A BACKENDS=( [azure]="$AZURE_GATEWAY" [do]="$DO_GATEWAY" )

is_healthy() {
  local url="$1"
  local code
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "${url}${HEALTH_PATH}" || echo 000)
  [ "$code" = "200" ]
}

# Build the pool of healthy backends.
POOL=()
for name in "${!BACKENDS[@]}"; do
  if is_healthy "${BACKENDS[$name]}"; then
    echo "[health] $name UP    -> ${BACKENDS[$name]}"
    POOL+=("$name")
  else
    echo "[health] $name DOWN  -> ${BACKENDS[$name]} (excluded)"
  fi
done

if [ "${#POOL[@]}" -eq 0 ]; then
  echo "[error] no healthy backends, aborting" >&2
  exit 1
fi

echo "[lb] dispatching $REQUESTS request(s) to $PATH_ across: ${POOL[*]}"
declare -A SERVED=()
for i in $(seq 1 "$REQUESTS"); do
  name="${POOL[$(( (i - 1) % ${#POOL[@]} ))]}"
  url="${BACKENDS[$name]}${PATH_}"
  code=$(curl -s -o /dev/null -w '%{http_code}' --max-time 10 "$url" || echo 000)
  SERVED[$name]=$(( ${SERVED[$name]:-0} + 1 ))
  printf '  req %2d -> %-5s %s [%s]\n' "$i" "$name" "$url" "$code"
done

echo "[lb] distribution:"
for name in "${!SERVED[@]}"; do
  printf '  %-5s %d\n' "$name" "${SERVED[$name]}"
done
