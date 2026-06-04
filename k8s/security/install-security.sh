#!/usr/bin/env bash
# Instala los controladores necesarios para TLS y aplica los recursos de seguridad.
# Uso: ./install-security.sh [namespace]   (por defecto: circleguard-stage)
set -euo pipefail

NS="${1:-circleguard-stage}"

echo ">> Instalando NGINX Ingress Controller..."
kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v1.10.1/deploy/static/provider/cloud/deploy.yaml
kubectl wait --namespace ingress-nginx \
  --for=condition=ready pod \
  --selector=app.kubernetes.io/component=controller \
  --timeout=180s

echo ">> Instalando cert-manager..."
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.5/cert-manager.yaml
kubectl wait --namespace cert-manager \
  --for=condition=ready pod --all \
  --timeout=180s

echo ">> Aplicando ClusterIssuer e Ingress TLS en el namespace ${NS}..."
kubectl apply -k k8s/security -n "${NS}"

echo ">> Listo. Verifica el certificado con: kubectl get certificate -n ${NS}"
